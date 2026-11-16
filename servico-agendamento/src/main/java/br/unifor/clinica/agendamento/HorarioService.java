package br.unifor.clinica.agendamento;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// Regras do horário de atendimento: 08:00 às 17:00, de hora em hora, segunda a sexta.
@Service
public class HorarioService {

    public static final int PRIMEIRA_HORA = 8;
    public static final int ULTIMA_HORA = 17;

    private final ProfissionalRepository profissionalRepository;
    private final ConsultaRepository consultaRepository;

    public HorarioService(ProfissionalRepository profissionalRepository, ConsultaRepository consultaRepository) {
        this.profissionalRepository = profissionalRepository;
        this.consultaRepository = consultaRepository;
    }

    public boolean ehFimDeSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    public void verificarProfissionalExiste(Long profissionalId) {
        if (!profissionalRepository.existsById(profissionalId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Profissional não encontrado");
        }
    }

    // Lança 400 se a dataHora não for hora cheia, dentro do expediente e no futuro
    public void validarHorarioDeAtendimento(LocalDateTime dataHora) {
        if (dataHora.getMinute() != 0 || dataHora.getSecond() != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O horário deve ser em hora cheia (ex.: 09:00)");
        }
        if (ehFimDeSemana(dataHora.toLocalDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Não há atendimento no fim de semana");
        }
        int hora = dataHora.getHour();
        if (hora < PRIMEIRA_HORA || hora > ULTIMA_HORA) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Horário fora do expediente (08:00 às 17:00)");
        }
        if (!dataHora.isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A consulta deve ser marcada para o futuro");
        }
    }

    // Lista os horários livres de um profissional num dia, ex.: ["08:00", "09:00", ...]
    public List<String> horariosLivres(Long profissionalId, LocalDate data) {
        verificarProfissionalExiste(profissionalId);

        List<String> livres = new ArrayList<>();
        if (ehFimDeSemana(data)) {
            return livres; // sem atendimento no fim de semana
        }

        // Horas já ocupadas por consultas não canceladas nesse dia
        Set<Integer> ocupadas = consultaRepository
                .findByProfissionalIdAndDataHoraBetweenAndStatusNot(profissionalId,
                        data.atStartOfDay(), data.atTime(23, 59), StatusConsulta.CANCELADA)
                .stream()
                .map(consulta -> consulta.getDataHora().getHour())
                .collect(Collectors.toSet());

        LocalDateTime agora = LocalDateTime.now();
        for (int hora = PRIMEIRA_HORA; hora <= ULTIMA_HORA; hora++) {
            LocalDateTime horario = data.atTime(hora, 0);
            if (horario.isAfter(agora) && !ocupadas.contains(hora)) { // futuro e livre
                livres.add(String.format("%02d:00", hora));
            }
        }
        return livres;
    }
}

package br.unifor.clinica.agendamento;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// Regras do horário de atendimento: 08:00 às 17:00, de hora em hora, segunda a sexta.
@Service
public class HorarioService {

    public static final int PRIMEIRA_HORA = 8;
    public static final int ULTIMA_HORA = 17;

    private final ProfissionalRepository profissionalRepository;

    public HorarioService(ProfissionalRepository profissionalRepository) {
        this.profissionalRepository = profissionalRepository;
    }

    public boolean ehFimDeSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    // Lista os horários livres de um profissional num dia, ex.: ["08:00", "09:00", ...]
    public List<String> horariosLivres(Long profissionalId, LocalDate data) {
        if (!profissionalRepository.existsById(profissionalId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Profissional não encontrado");
        }

        List<String> livres = new ArrayList<>();
        if (ehFimDeSemana(data)) {
            return livres; // sem atendimento no fim de semana
        }

        LocalDateTime agora = LocalDateTime.now();
        for (int hora = PRIMEIRA_HORA; hora <= ULTIMA_HORA; hora++) {
            LocalDateTime horario = data.atTime(hora, 0);
            if (horario.isAfter(agora)) { // só horários no futuro
                livres.add(String.format("%02d:00", hora));
            }
        }
        return livres;
    }
}

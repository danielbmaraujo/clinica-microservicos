package br.unifor.clinica.agendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// Regras de negócio das consultas.
@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final HorarioService horarioService;
    private final PacientesClient pacientesClient;
    private final NotificacoesClient notificacoesClient;

    public ConsultaService(ConsultaRepository consultaRepository, HorarioService horarioService,
            PacientesClient pacientesClient, NotificacoesClient notificacoesClient) {
        this.consultaRepository = consultaRepository;
        this.horarioService = horarioService;
        this.pacientesClient = pacientesClient;
        this.notificacoesClient = notificacoesClient;
    }

    public Consulta marcar(MarcarConsultaRequest dados) {
        // 1. Profissional existe? (banco deste serviço)
        horarioService.verificarProfissionalExiste(dados.profissionalId());

        // 2. Horário válido? (hora cheia, expediente, futuro)
        horarioService.validarHorarioDeAtendimento(dados.dataHora());

        // 3. Paciente existe e está ativo? (pergunta ao Serviço de Pacientes pela rede)
        PacienteDTO paciente = pacientesClient.buscarPaciente(dados.pacienteId());
        if (!paciente.ativo()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paciente inativo");
        }

        // 4. Profissional já tem consulta nesse horário?
        if (consultaRepository.existsByProfissionalIdAndDataHoraAndStatusNot(
                dados.profissionalId(), dados.dataHora(), StatusConsulta.CANCELADA)) {
            throw new ApiException(HttpStatus.CONFLICT, "O profissional já tem consulta neste horário");
        }

        Consulta consulta = new Consulta();
        consulta.setPacienteId(dados.pacienteId());
        consulta.setProfissionalId(dados.profissionalId());
        consulta.setDataHora(dados.dataHora());
        consulta.setStatus(StatusConsulta.AGENDADA);
        Consulta salva = consultaRepository.save(consulta);

        // 5. Avisa o Serviço de Notificações (se ele estiver fora, a consulta continua valendo)
        notificacoesClient.avisarMarcacao(salva);
        return salva;
    }

    public Consulta remarcar(Long id, RemarcarConsultaRequest dados) {
        Consulta consulta = buscar(id);
        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Consulta cancelada não pode ser remarcada");
        }

        horarioService.validarHorarioDeAtendimento(dados.dataHora());

        // Conflito com outra consulta do mesmo profissional (ignora a própria consulta)
        if (consultaRepository.existsByProfissionalIdAndDataHoraAndStatusNotAndIdNot(
                consulta.getProfissionalId(), dados.dataHora(), StatusConsulta.CANCELADA, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "O profissional já tem consulta neste horário");
        }

        consulta.setDataHora(dados.dataHora());
        consulta.setStatus(StatusConsulta.REMARCADA);
        Consulta salva = consultaRepository.save(consulta);
        notificacoesClient.avisarRemarcacao(salva);
        return salva;
    }

    public Consulta cancelar(Long id) {
        Consulta consulta = buscar(id);
        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Consulta já está cancelada");
        }
        consulta.setStatus(StatusConsulta.CANCELADA);
        Consulta salva = consultaRepository.save(consulta);
        notificacoesClient.avisarCancelamento(salva);
        return salva;
    }

    public Consulta buscar(Long id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Consulta não encontrada"));
    }

    // Filtros opcionais: pacienteId e/ou data
    public List<Consulta> listar(Long pacienteId, LocalDate data) {
        if (data == null) {
            return pacienteId == null
                    ? consultaRepository.findAllByOrderByDataHora()
                    : consultaRepository.findByPacienteIdOrderByDataHora(pacienteId);
        }
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(23, 59);
        return pacienteId == null
                ? consultaRepository.findByDataHoraBetweenOrderByDataHora(inicio, fim)
                : consultaRepository.findByPacienteIdAndDataHoraBetweenOrderByDataHora(pacienteId, inicio, fim);
    }
}

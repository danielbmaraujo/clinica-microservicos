package br.unifor.clinica.agendamento;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// O Spring Data gera o SQL de cada método a partir do nome.
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    // Conflito: já existe consulta não cancelada desse profissional nesse horário?
    boolean existsByProfissionalIdAndDataHoraAndStatusNot(Long profissionalId, LocalDateTime dataHora,
            StatusConsulta status);

    // Mesmo teste, ignorando a própria consulta (usado ao remarcar)
    boolean existsByProfissionalIdAndDataHoraAndStatusNotAndIdNot(Long profissionalId, LocalDateTime dataHora,
            StatusConsulta status, Long id);

    // Consultas não canceladas de um profissional num intervalo (para calcular horários livres)
    List<Consulta> findByProfissionalIdAndDataHoraBetweenAndStatusNot(Long profissionalId, LocalDateTime inicio,
            LocalDateTime fim, StatusConsulta status);

    // Listagens com filtros opcionais
    List<Consulta> findAllByOrderByDataHora();

    List<Consulta> findByPacienteIdOrderByDataHora(Long pacienteId);

    List<Consulta> findByDataHoraBetweenOrderByDataHora(LocalDateTime inicio, LocalDateTime fim);

    List<Consulta> findByPacienteIdAndDataHoraBetweenOrderByDataHora(Long pacienteId, LocalDateTime inicio,
            LocalDateTime fim);
}

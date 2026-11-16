package br.unifor.clinica.agendamento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// Acesso ao banco: o Spring Data gera o SQL a partir do nome do método.
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findByEspecialidadeContainingIgnoreCase(String especialidade);
}

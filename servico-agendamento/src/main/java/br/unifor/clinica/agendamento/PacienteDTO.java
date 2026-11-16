package br.unifor.clinica.agendamento;

// Parte do JSON do Serviço de Pacientes que nos interessa (os outros campos são ignorados).
public record PacienteDTO(Long id, String nome, boolean ativo) {
}

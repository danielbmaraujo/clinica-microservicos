package br.unifor.clinica.agendamento;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

// JSON recebido em POST /consultas: {"pacienteId": 1, "profissionalId": 1, "dataHora": "2026-11-20T09:00"}
public record MarcarConsultaRequest(
        @NotNull(message = "pacienteId é obrigatório") Long pacienteId,
        @NotNull(message = "profissionalId é obrigatório") Long profissionalId,
        @NotNull(message = "dataHora é obrigatória") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime dataHora) {
}

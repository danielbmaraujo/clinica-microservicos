package br.unifor.clinica.agendamento;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

// JSON recebido em PUT /consultas/{id}/remarcar: {"dataHora": "2026-11-21T10:00"}
public record RemarcarConsultaRequest(
        @NotNull(message = "dataHora é obrigatória") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime dataHora) {
}

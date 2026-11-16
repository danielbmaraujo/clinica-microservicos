package br.unifor.clinica.agendamento;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Cliente HTTP que avisa o Serviço de Notificações PELA REDE.
// Se o Notificações estiver fora do ar, NÃO dá erro: a consulta continua valendo, só registra um aviso no log.
@Component
public class NotificacoesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacoesClient.class);

    // Formato usado nas mensagens: "20/11/2026 às 09:00"
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final RestClient restClient;

    // A URL vem da variável de ambiente NOTIFICACOES_URL (ver application.properties)
    public NotificacoesClient(@Value("${notificacoes.url}") String notificacoesUrl) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3000);
        fabrica.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(notificacoesUrl)
                .requestFactory(fabrica)
                .build();
    }

    public void avisarMarcacao(Consulta consulta) {
        enviar(consulta, "CONFIRMACAO",
                "Sua consulta foi marcada para " + consulta.getDataHora().format(FORMATO) + ".");
    }

    public void avisarRemarcacao(Consulta consulta) {
        enviar(consulta, "REMARCACAO",
                "Sua consulta foi remarcada para " + consulta.getDataHora().format(FORMATO) + ".");
    }

    public void avisarCancelamento(Consulta consulta) {
        enviar(consulta, "CANCELAMENTO",
                "Sua consulta de " + consulta.getDataHora().format(FORMATO) + " foi cancelada.");
    }

    // POST {NOTIFICACOES_URL}/notificacoes  {pacienteId, consultaId, tipo, mensagem}
    private void enviar(Consulta consulta, String tipo, String mensagem) {
        Map<String, Object> corpo = Map.of(
                "pacienteId", consulta.getPacienteId(),
                "consultaId", consulta.getId(),
                "tipo", tipo,
                "mensagem", mensagem);
        try {
            restClient.post()
                    .uri("/notificacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // Falha no aviso não desfaz a consulta
            log.warn("Não foi possível enviar a notificação da consulta {} ({}): {}",
                    consulta.getId(), tipo, e.getMessage());
        }
    }
}

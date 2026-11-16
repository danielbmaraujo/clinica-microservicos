package br.unifor.clinica.agendamento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Cliente HTTP que conversa com o Serviço de Pacientes PELA REDE (não acessa o banco dele).
@Component
public class PacientesClient {

    private static final Logger log = LoggerFactory.getLogger(PacientesClient.class);

    private final RestClient restClient;

    // A URL vem da variável de ambiente PACIENTES_URL (ver application.properties)
    public PacientesClient(@Value("${pacientes.url}") String pacientesUrl) {
        // Timeout de 3 segundos para não travar se o serviço não responder
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3000);
        fabrica.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(pacientesUrl)
                .requestFactory(fabrica)
                .build();
    }

    // GET {PACIENTES_URL}/pacientes/{id}
    public PacienteDTO buscarPaciente(Long id) {
        try {
            return restClient.get()
                    .uri("/pacientes/{id}", id)
                    .retrieve()
                    .body(PacienteDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            // O Serviço de Pacientes respondeu 404
            throw new ApiException(HttpStatus.NOT_FOUND, "Paciente não encontrado");
        } catch (RestClientException e) {
            // Serviço fora do ar, timeout ou erro inesperado
            log.warn("Falha ao chamar o Serviço de Pacientes: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de Pacientes indisponível");
        }
    }
}

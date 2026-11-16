package br.unifor.clinica.agendamento;

import org.springframework.http.HttpStatus;

// Erro de regra de negócio com o status HTTP que deve ser devolvido.
// Ex.: throw new ApiException(HttpStatus.NOT_FOUND, "Profissional não encontrado");
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

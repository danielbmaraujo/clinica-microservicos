package br.unifor.clinica.agendamento;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // GET /health -> {"status": "ok", "servico": "agendamento"}
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "servico", "agendamento");
    }
}

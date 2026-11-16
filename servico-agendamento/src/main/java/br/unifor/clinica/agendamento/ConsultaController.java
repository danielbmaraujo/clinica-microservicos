package br.unifor.clinica.agendamento;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    // POST /consultas -> 201 · 400 · 404 · 409 · 503
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Consulta marcar(@Valid @RequestBody MarcarConsultaRequest dados) {
        return consultaService.marcar(dados);
    }

    // GET /consultas?pacienteId=1&data=2026-11-20 (filtros opcionais)
    @GetMapping
    public List<Consulta> listar(@RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return consultaService.listar(pacienteId, data);
    }

    // GET /consultas/{id}
    @GetMapping("/{id}")
    public Consulta detalhar(@PathVariable Long id) {
        return consultaService.buscar(id);
    }

    // PUT /consultas/{id}/remarcar  {"dataHora": "..."}
    @PutMapping("/{id}/remarcar")
    public Consulta remarcar(@PathVariable Long id, @Valid @RequestBody RemarcarConsultaRequest dados) {
        return consultaService.remarcar(id, dados);
    }

    // PUT /consultas/{id}/cancelar
    @PutMapping("/{id}/cancelar")
    public Consulta cancelar(@PathVariable Long id) {
        return consultaService.cancelar(id);
    }
}

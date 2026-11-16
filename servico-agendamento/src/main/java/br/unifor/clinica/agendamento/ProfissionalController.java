package br.unifor.clinica.agendamento;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/profissionais")
public class ProfissionalController {

    private final ProfissionalRepository repository;

    public ProfissionalController(ProfissionalRepository repository) {
        this.repository = repository;
    }

    // POST /profissionais -> 201 (ou 400 se faltar nome/especialidade)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Profissional cadastrar(@Valid @RequestBody Profissional profissional) {
        profissional.setId(null); // o id é sempre gerado pelo banco
        return repository.save(profissional);
    }

    // GET /profissionais?especialidade=Fisio (filtro opcional)
    @GetMapping
    public List<Profissional> listar(@RequestParam(required = false) String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            return repository.findAll();
        }
        return repository.findByEspecialidadeContainingIgnoreCase(especialidade);
    }
}

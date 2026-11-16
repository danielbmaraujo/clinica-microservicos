package br.unifor.clinica.agendamento;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    // GET /horarios-livres?profissionalId=1&data=2026-11-20
    // @DateTimeFormat garante o formato YYYY-MM-DD (senão o Spring aceitaria o formato do idioma do sistema)
    @GetMapping("/horarios-livres")
    public List<String> horariosLivres(@RequestParam Long profissionalId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return horarioService.horariosLivres(profissionalId, data);
    }
}

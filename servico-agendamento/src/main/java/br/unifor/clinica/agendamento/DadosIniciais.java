package br.unifor.clinica.agendamento;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Ao iniciar, cadastra 3 profissionais de exemplo se a tabela estiver vazia.
@Component
public class DadosIniciais implements CommandLineRunner {

    private final ProfissionalRepository repository;

    public DadosIniciais(ProfissionalRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                    new Profissional("Dr. Carlos Lima", "Fisioterapia"),
                    new Profissional("Dra. Beatriz Rocha", "Cardiologia"),
                    new Profissional("Dr. Marcos Alves", "Clínica Geral")));
        }
    }
}

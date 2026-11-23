package br.unifor.clinica.agendamento;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Libera o frontend React local (localhost:3000) e o frontend publicado (variável FRONTEND_URL).
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Vem de frontend.url=${FRONTEND_URL:} no application.properties (vazio se não definida)
    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origens = new ArrayList<>();
        origens.add("http://localhost:3000");
        if (!frontendUrl.isBlank()) {
            origens.add(frontendUrl.replaceAll("/$", ""));
        }

        registry.addMapping("/**")
                .allowedOrigins(origens.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}

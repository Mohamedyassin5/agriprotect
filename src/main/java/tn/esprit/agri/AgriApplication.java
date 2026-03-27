package tn.esprit.agri;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class AgriApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriApplication.class, args);
    }

    // ← Tout ce qui suit est à l'intérieur de la classe → OK
    @Bean
    CommandLineRunner debugProfile(Environment env) {
        return args -> {
            System.out.println("Profiles actifs : " + Arrays.toString(env.getActiveProfiles()));
        };
    }

    // Tu peux ajouter d'autres @Bean ici si besoin
}
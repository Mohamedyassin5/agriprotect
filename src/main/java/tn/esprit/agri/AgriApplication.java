package tn.esprit.agri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgriApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriApplication.class, args);
    }

}

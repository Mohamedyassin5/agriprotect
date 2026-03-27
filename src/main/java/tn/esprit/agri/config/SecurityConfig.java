package tn.esprit.agri.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tn.esprit.agri.security.JwtAuthenticationFilter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ====================== PUBLIC ======================
                        .requestMatchers(
                                "/", "/index.html", "/sign-contract.html", "/payment.html",
                                "/payment-success.html", "/payment-cancel.html",
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**",
                                "/agri/auth/**",
                                "/stripe/webhook"
                        ).permitAll()

                        // Endpoints accessibles via lien email (sans JWT obligatoire)
                        .requestMatchers(
                                "/agri/phase1/*/sign/token",
                                "/agri/phase1/*/payment-info",
                                "/agri/phase1/*/create-checkout"
                        ).permitAll()

                        // ====================== ADMIN ======================
                        .requestMatchers("/agri/admin/**").hasRole("ADMIN")

                        // ====================== SUBSCRIBE ======================
                        .requestMatchers(HttpMethod.POST, "/agri/phase1/subscribe").authenticated()

                        // ====================== PAYMENT ENDPOINT ======================
                        // ←←← AJOUT IMPORTANT ICI
                        .requestMatchers(HttpMethod.POST, "/agri/phase1/pay/**").authenticated()

                        // ====================== AUTRES PHASE1 ======================
                        .requestMatchers("/agri/phase1/**").hasAnyRole("FARMER", "ADMIN")

                        // Assurances
                        .requestMatchers("/agri/insurances/**").hasAnyRole("FARMER", "ADMIN")
                        .requestMatchers("/stripe/webhook").permitAll()
                        // Tout le reste
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @PostConstruct
    public void debugMethodSecurity() {
        System.out.println(">>> METHOD SECURITY ACTIVATED: @PreAuthorize & @PostAuthorize sont actifs");
    }
}

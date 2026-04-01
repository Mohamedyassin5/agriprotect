package tn.esprit.agri.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import tn.esprit.agri.security.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
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
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/sign-contract.html", "/payment.html",
                                "/payment-success.html", "/payment-cancel.html","/regularize-payment.html",
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/agri/auth/**", "/stripe/webhook"
                        ).permitAll()
                        // Public auth endpoints
                        .requestMatchers(
                                "/agri/auth/login",
                                "/agri/auth/forgot-password",
                                "/agri/auth/reset-password",
                                "/agri/auth/face/login",
                                "/agri/assistant/health"
                        ).permitAll()
                        // Endpoints spéciaux sans JWT (déjà présents)
                        .requestMatchers(
                                "/agri/phase1/*/sign/token",
                                "/agri/phase1/*/payment-info",
                                "/agri/phase1/*/create-checkout"
                        ).permitAll()

                       
                        
                        // Enforce authentication for face enrollment (redundant with anyRequest().authenticated() but explicit is better)
                        .requestMatchers("/agri/auth/face/enroll").authenticated()

                        // Public register ONLY (POST /agri/users)
                        .requestMatchers(HttpMethod.POST, "/agri/users").permitAll()

                        // Swagger / docs
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()

                        // If you want AI recommend public (optional)
                        .requestMatchers(HttpMethod.POST, "/agri/crops/recommend").permitAll()

                        // Everything else requires JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // for H2 console frames
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .build();
    }
}


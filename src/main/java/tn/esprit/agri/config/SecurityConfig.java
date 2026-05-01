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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import tn.esprit.agri.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/sign-contract.html", "/payment.html",
                                "/payment-success.html", "/payment-cancel.html","/regularize-payment.html",
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/agri/auth/**", "/stripe/webhook",
                                "/api/payments/**", "/api/solidarity-funds/**"
                        ).permitAll()
                        // Public auth endpoints
                        .requestMatchers(
                                "/agri/auth/login",
                                "/agri/auth/forgot-password",
                                "/agri/auth/reset-password",
                                "/agri/auth/face/login",
                                "/agri/assistant/health"
                        ).permitAll()


                        .requestMatchers(
                                "/agri/phase1/*/sign/token",
                                "/agri/phase1/*/payment-info",
                                "/agri/phase1/*/create-checkout"
                        ).permitAll()


                        // Enforce authentication for face enrollment (redundant with anyRequest().authenticated() but explicit is better)
                                .requestMatchers("/agri/auth/**", "/agri/users/**", "/agri/crops/**", "/api/payments/**", "/api/solidarity-funds/**").permitAll()

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
                        
                        // Explicitly authorize crops management
                        .requestMatchers("/agri/crops/**").hasAnyRole("ADMIN", "FARMER")
                        
                        // Everything else requires JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // for H2 console frames
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


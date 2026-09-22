package pe.brujula.config;

import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import pe.brujula.repository.FinanceRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService(FinanceRepository repo) {
        return email -> repo.user(email.strip().toLowerCase(Locale.ROOT))
            .map(u -> User.withUsername(u.email()).password(u.passwordHash()).roles(u.role()).disabled(!u.enabled()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/api/csrf", "/api/register", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(login -> login.loginProcessingUrl("/api/login").usernameParameter("email")
                .successHandler((req,res,a) -> res.setStatus(204))
                .failureHandler((req,res,e) -> res.sendError(401,"Correo o contraseña incorrectos")))
            .logout(logout -> logout.logoutUrl("/api/logout").logoutSuccessHandler((req,res,a) -> res.setStatus(204)))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.setStatus(HttpStatus.UNAUTHORIZED.value())))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
        return http.build();
    }
}

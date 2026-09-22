package pe.brujula.config;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.brujula.repository.FinanceRepository;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final FinanceRepository repo;
    private final String email;
    public AdminBootstrap(FinanceRepository repo,@Value("${brujula.admin-email:}") String email) { this.repo=repo;this.email=email; }
    @Override public void run(ApplicationArguments args) {
        if(!email.isBlank() && repo.promote(email.strip().toLowerCase(Locale.ROOT))==0)
            throw new IllegalStateException("La cuenta indicada para administrar no existe. Regístrala primero y vuelve a iniciar.");
    }
}

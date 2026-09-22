package pe.brujula;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.brujula.model.Models.*;
import pe.brujula.repository.FinanceRepository;
import pe.brujula.service.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:brujula_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class FinanceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FinanceService service;
    @Autowired FinanceRepository repo;
    @Autowired ReportService reports;
    @Autowired JdbcClient db;
    private String account() {
        String email=UUID.randomUUID()+"@example.test";
        service.register(new Register("Persona de prueba",email,"Prueba-segura-2026"));return email;
    }
    private long category(String email) {return repo.categories(service.user(email).id()).stream().filter(c->c.kind().equals("GASTO")).findFirst().orElseThrow().id();}
    private MovementInput input(long category) {return new MovementInput(category,"Almuerzo",new BigDecimal("25.50"),LocalDate.now().minusDays(1),"Yape");}
    @Test void registrationLoginAndPasswordHash() throws Exception {
        String email=UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(new Register("Piero",email,"Prueba-segura-2026")))).andExpect(status().isCreated());
        assertThat(repo.user(email).orElseThrow().passwordHash()).startsWith("$2a$").doesNotContain("Prueba");
        mvc.perform(post("/api/login").with(csrf()).param("email",email).param("password","Prueba-segura-2026")).andExpect(status().isNoContent());
        mvc.perform(post("/api/login").with(csrf()).param("email",email).param("password","incorrecta")).andExpect(status().isUnauthorized());
    }
    @Test void anonymousAndCsrfProtection() throws Exception {
        mvc.perform(get("/api/state")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/movements").with(user(account())).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }
    @Test void secondAccountCannotReadChangeOrDeleteFirstAccountsMovement() throws Exception {
        String a=account(),b=account();service.saveMovement(a,null,input(category(a)));
        long id=repo.movements(service.user(a).id()).get(0).id();
        mvc.perform(get("/api/state").with(user(b))).andExpect(status().isOk()).andExpect(jsonPath("$.movements").isEmpty());
        mvc.perform(put("/api/movements/"+id).with(user(b)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input(category(b))))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/movements/"+id).with(user(b)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/movements").with(user(b)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input(category(a))))).andExpect(status().isNotFound());
        assertThat(repo.movements(service.user(a).id())).hasSize(1);
    }
    @Test void ownerCanCreateUpdateDeleteAndExactDecimalsPersist() throws Exception {
        String a=account();
        mvc.perform(post("/api/movements").with(user(a)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input(category(a))))).andExpect(status().isCreated());
        var first=repo.movements(service.user(a).id()).get(0);assertThat(first.amount()).isEqualByComparingTo("25.50");
        var edited=new MovementInput(category(a),"Cena",new BigDecimal("30.10"),LocalDate.now().minusDays(1),"Tarjeta");
        mvc.perform(put("/api/movements/"+first.id()).with(user(a)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(edited))).andExpect(status().isOk());
        assertThat(repo.movements(service.user(a).id()).get(0).amount()).isEqualByComparingTo("30.10");
        mvc.perform(delete("/api/movements/"+first.id()).with(user(a)).with(csrf())).andExpect(status().isNoContent());
        assertThat(repo.movements(service.user(a).id())).isEmpty();
    }
    @Test void invalidMoneyAndFutureDateRejected() throws Exception {
        String a=account();
        for(var in:new MovementInput[]{new MovementInput(category(a),"Error",new BigDecimal("-5"),LocalDate.now(),"Yape"),new MovementInput(category(a),"Error",new BigDecimal("5.001"),LocalDate.now(),"Yape"),new MovementInput(category(a),"Futuro",BigDecimal.TEN,LocalDate.now().plusDays(2),"Yape")})
            mvc.perform(post("/api/movements").with(user(a)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(in))).andExpect(status().isBadRequest());
    }
    @Test void budgetsAndGoalsStayPrivate() throws Exception {
        String a=account(),b=account();
        service.saveBudget(a,new BudgetInput(category(a),"2026-09",new BigDecimal("300")));
        service.saveBudget(a,new BudgetInput(category(a),"2026-09",new BigDecimal("400")));
        assertThat(repo.budgets(service.user(a).id())).hasSize(1);
        service.saveGoal(a,null,new GoalInput("Laptop",new BigDecimal("3000"),new BigDecimal("400"),LocalDate.now().plusMonths(4)));
        long goal=repo.goals(service.user(a).id()).get(0).id(),budget=repo.budgets(service.user(a).id()).get(0).id();
        mvc.perform(delete("/api/goals/"+goal).with(user(b)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/budgets/"+budget).with(user(b)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/state").with(user(b))).andExpect(jsonPath("$.budgets").isEmpty()).andExpect(jsonPath("$.goals").isEmpty());
    }
    @Test void categoryInUseCannotBeDeletedAndDuplicateEmailRejected() throws Exception {
        String a=account();service.saveMovement(a,null,input(category(a)));
        mvc.perform(delete("/api/categories/"+category(a)).with(user(a)).with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/api/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Register("Otro",a,"Prueba-segura-2026")))).andExpect(status().isConflict());
    }
    @Test void allReportsAndEmptyReportAreRealPdfs() throws Exception {
        String a=account();service.sample(a);String month=YearMonth.now(ZoneId.of("America/Lima")).toString();
        for(String type:new String[]{"movements","categories","summary"}) {
            byte[] pdf=reports.generate(a,month,type);assertThat(new String(pdf,0,5)).isEqualTo("%PDF-");assertThat(pdf.length).isGreaterThan(1000);
            java.nio.file.Files.write(java.nio.file.Path.of("target/example-"+type+".pdf"),pdf);
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/qa-state.json"),json.writeValueAsString(service.state(a)));
        byte[] empty=reports.generate(account(),month,"movements");assertThat(new String(empty,0,5)).isEqualTo("%PDF-");
    }
    @Test void adminCanDisableButRegularUserCannotAdminister() throws Exception {
        String admin=account(),regular=account();repo.promote(admin);
        long regularId=service.user(regular).id();
        mvc.perform(get("/api/admin/users").with(user(regular))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(user(admin))).andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(patch("/api/admin/users/"+regularId).with(user(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/state").with(user(regular))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").with(csrf()).param("email",regular).param("password","Prueba-segura-2026")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/users/"+service.user(admin).id()).with(user(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/users/"+regularId).with(user(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/state").with(user(regular))).andExpect(status().isOk());
    }
}

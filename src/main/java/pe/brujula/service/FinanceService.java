package pe.brujula.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.brujula.model.Models.*;
import pe.brujula.repository.FinanceRepository;

@Service
public class FinanceService {
    private final FinanceRepository repo;
    private final PasswordEncoder encoder;
    public FinanceService(FinanceRepository repo,PasswordEncoder encoder) { this.repo=repo; this.encoder=encoder; }
    public User user(String email) {
        User user=repo.user(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(!user.enabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"La cuenta está desactivada.");
        return user;
    }
    @Transactional
    public void register(Register input) {
        if(input.password().getBytes(StandardCharsets.UTF_8).length > 72) throw bad("La contraseña no puede superar 72 bytes.");
        repo.createUser(input,encoder.encode(input.password()));
        long uid=user(input.email().strip().toLowerCase(Locale.ROOT)).id();
        for(String name:List.of("Alimentación","Transporte","Hogar","Estudios","Salud","Entretenimiento","Compras","Otros gastos"))
            repo.createCategory(uid,new CategoryInput(name,"GASTO"));
        for(String name:List.of("Sueldo","Trabajo independiente","Otros ingresos"))
            repo.createCategory(uid,new CategoryInput(name,"INGRESO"));
    }
    public Map<String,Object> state(String email) {
        User u=user(email);
        return Map.of("user",Map.of("name",u.name(),"email",u.email(),"role",u.role()),"categories",repo.categories(u.id()),
            "movements",repo.movements(u.id()),"budgets",repo.budgets(u.id()),"goals",repo.goals(u.id()));
    }
    private Category category(long uid,long id) {
        return repo.categories(uid).stream().filter(c -> c.id()==id).findFirst().orElseThrow(() -> missing());
    }
    public void saveMovement(String email,Long id,MovementInput input) {
        long uid=user(email).id(); category(uid,input.categoryId());
        if(input.date().isBefore(LocalDate.of(1900,1,1))) throw bad("Revisa la fecha del movimiento.");
        check(repo.saveMovement(uid,id,input));
    }
    public void saveBudget(String email,BudgetInput input) {
        long uid=user(email).id();
        if(!category(uid,input.categoryId()).kind().equals("GASTO")) throw bad("Elige una categoría de gastos.");
        try { YearMonth.parse(input.month()); } catch(Exception e) { throw bad("El mes no es válido."); }
        repo.saveBudget(uid,input);
    }
    public void saveGoal(String email,Long id,GoalInput input) {
        if(input.saved().compareTo(input.target())>0) throw bad("El ahorro reservado no puede superar la meta.");
        check(repo.saveGoal(user(email).id(),id,input));
    }
    public void createCategory(String email,CategoryInput input) { repo.createCategory(user(email).id(),input); }
    public void delete(String email,String type,long id) {
        long uid=user(email).id();
        int changed=switch(type) {
            case "movements" -> repo.deleteMovement(uid,id);
            case "categories" -> repo.deleteCategory(uid,id);
            case "budgets" -> repo.deleteBudget(uid,id);
            case "goals" -> repo.deleteGoal(uid,id);
            default -> throw missing();
        };
        check(changed);
    }
    @Transactional
    public void sample(String email) {
        long uid=user(email).id();
        if(!repo.movements(uid).isEmpty() || !repo.budgets(uid).isEmpty() || !repo.goals(uid).isEmpty())
            throw bad("Los ejemplos solo se pueden cargar en una cuenta sin movimientos, presupuestos ni metas.");
        Map<String,Long> cats=new HashMap<>(); repo.categories(uid).forEach(c -> cats.put(c.name(),c.id()));
        for(String name:List.of("Sueldo","Alimentación","Transporte","Hogar","Estudios","Entretenimiento")) {
            if(!cats.containsKey(name)) {
                repo.createCategory(uid,new CategoryInput(name,name.equals("Sueldo")?"INGRESO":"GASTO"));
            }
        }
        repo.categories(uid).forEach(c -> cats.put(c.name(),c.id()));
        YearMonth now=YearMonth.now(ZoneId.of("America/Lima"));
        for(int i=5;i>=0;i--) {
            LocalDate date=now.minusMonths(i).atDay(1);
            add(uid,cats.get("Sueldo"),"Sueldo mensual",2800+(5-i)*120,date,"Transferencia");
            add(uid,cats.get("Hogar"),"Alquiler y servicios",850,date,"Transferencia");
            add(uid,cats.get("Alimentación"),"Compras del mes",340+(5-i)*18,date,"Tarjeta");
            add(uid,cats.get("Transporte"),"Movilidad",130+(5-i)*8,date,"Yape");
            add(uid,cats.get("Estudios"),"Curso de programación",180,date,"Tarjeta");
            add(uid,cats.get("Entretenimiento"),"Salida de fin de semana",90+(5-i)*5,date,"Yape");
        }
        repo.saveBudget(uid,new BudgetInput(cats.get("Alimentación"),now.toString(),new BigDecimal("600")));
        repo.saveBudget(uid,new BudgetInput(cats.get("Transporte"),now.toString(),new BigDecimal("220")));
        repo.saveBudget(uid,new BudgetInput(cats.get("Entretenimiento"),now.toString(),new BigDecimal("200")));
        repo.saveGoal(uid,null,new GoalInput("Mi próxima laptop",new BigDecimal("4500"),new BigDecimal("1800"),now.plusMonths(5).atEndOfMonth()));
        repo.saveGoal(uid,null,new GoalInput("Fondo de emergencia",new BigDecimal("6000"),new BigDecimal("900"),now.plusMonths(9).atEndOfMonth()));
    }
    private void add(long uid,long cat,String desc,int amount,LocalDate date,String method) {
        repo.saveMovement(uid,null,new MovementInput(cat,desc,BigDecimal.valueOf(amount),date,method));
    }
    public List<AdminUser> adminUsers(String email) { requireAdmin(email);return repo.users(); }
    public void setAccountStatus(String email,long id,boolean enabled) {
        User admin=requireAdmin(email);
        if(admin.id()==id) throw bad("No puedes desactivar tu propia cuenta.");
        AdminUser target=repo.users().stream().filter(u->u.id()==id).findFirst().orElseThrow(()->missing());
        if(target.role().equals("ADMIN")) throw bad("Las cuentas administradoras se gestionan desde la configuración local.");
        check(repo.setEnabled(id,enabled));
    }
    private User requireAdmin(String email) {
        User user=user(email);
        if(!user.role().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso solo para administradores.");
        return user;
    }
    private void check(int changed) { if(changed==0) throw missing(); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"No se encontró el registro."); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}

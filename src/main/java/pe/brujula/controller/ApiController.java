package pe.brujula.controller;

import java.security.Principal;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import pe.brujula.model.Models.*;
import pe.brujula.service.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final FinanceService service;
    private final ReportService reports;
    public ApiController(FinanceService service,ReportService reports) { this.service=service; this.reports=reports; }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody Register input) { service.register(input); }
    @GetMapping("/state") public Map<String,Object> state(Principal p) { return service.state(p.getName()); }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public void category(Principal p,@Valid @RequestBody CategoryInput in) { service.createCategory(p.getName(),in); }
    @PostMapping("/movements") @ResponseStatus(HttpStatus.CREATED)
    public void movement(Principal p,@Valid @RequestBody MovementInput in) { service.saveMovement(p.getName(),null,in); }
    @PutMapping("/movements/{id}")
    public void movement(Principal p,@PathVariable long id,@Valid @RequestBody MovementInput in) { service.saveMovement(p.getName(),id,in); }
    @PostMapping("/budgets")
    public void budget(Principal p,@Valid @RequestBody BudgetInput in) { service.saveBudget(p.getName(),in); }
    @PostMapping("/goals") @ResponseStatus(HttpStatus.CREATED)
    public void goal(Principal p,@Valid @RequestBody GoalInput in) { service.saveGoal(p.getName(),null,in); }
    @PutMapping("/goals/{id}")
    public void goal(Principal p,@PathVariable long id,@Valid @RequestBody GoalInput in) { service.saveGoal(p.getName(),id,in); }
    @DeleteMapping("/{type:movements|categories|budgets|goals}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal p,@PathVariable String type,@PathVariable long id) { service.delete(p.getName(),type,id); }
    @PostMapping("/sample") public void sample(Principal p) { service.sample(p.getName()); }
    @GetMapping("/admin/users") public java.util.List<AdminUser> users(Principal p) { return service.adminUsers(p.getName()); }
    @PatchMapping("/admin/users/{id}")
    public void accountStatus(Principal p,@PathVariable long id,@Valid @RequestBody AccountStatus input) { service.setAccountStatus(p.getName(),id,input.enabled()); }
    @GetMapping("/reports") public ResponseEntity<byte[]> report(Principal p,@RequestParam String month,
                                                               @RequestParam(defaultValue="movements") String type) throws Exception {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=brujula-"+type.replaceAll("[^a-z]","")+".pdf")
            .body(reports.generate(p.getName(),month,type));
    }
}

package pe.brujula.service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import pe.brujula.model.Models.*;
import pe.brujula.repository.FinanceRepository;

@Service
public class ReportService {
    private final FinanceRepository repo;
    private final FinanceService finance;
    private volatile JasperReport compiled;
    public ReportService(FinanceRepository repo,FinanceService finance) { this.repo=repo;this.finance=finance; }
    private synchronized JasperReport template() throws Exception {
        if(compiled==null) try(var in=new ClassPathResource("reports/monthly.jrxml").getInputStream()) { compiled=JasperCompileManager.compileReport(in); }
        return compiled;
    }
    public byte[] generate(String email,String month,String type) throws Exception {
        YearMonth period;
        try { period=YearMonth.parse(month); } catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Elige un mes válido."); }
        if(!List.of("movements","categories","summary").contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reporte no válido.");
        User user=finance.user(email);
        var rows=repo.movements(user.id()).stream().filter(m -> YearMonth.from(m.date()).equals(period)).toList();
        BigDecimal income=rows.stream().filter(m -> m.kind().equals("INGRESO")).map(Movement::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal expense=rows.stream().filter(m -> m.kind().equals("GASTO")).map(Movement::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
        Collection<Map<String,?>> data=new ArrayList<>();
        if(type.equals("movements")) {
            for(var m:rows) data.add(Map.of("date",m.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                "description",m.description(),"category",m.category(),"kind",m.kind(),"amount",m.amount()));
        } else if(type.equals("categories")) {
            Map<String,BigDecimal> totals=new TreeMap<>();
            rows.stream().filter(m -> m.kind().equals("GASTO")).forEach(m -> totals.merge(m.category(),m.amount(),BigDecimal::add));
            totals.forEach((name,amount) -> data.add(Map.of("date",month,"description",name,"category","Gastos por categoría","kind","GASTO","amount",amount)));
        } else {
            data.add(Map.of("date",month,"description","Ingresos del mes","category","Resumen","kind","INGRESO","amount",income));
            data.add(Map.of("date",month,"description","Gastos del mes","category","Resumen","kind","GASTO","amount",expense));
            data.add(Map.of("date",month,"description","Balance del mes","category","Ingresos menos gastos","kind","BALANCE","amount",income.subtract(expense)));
        }
        Map<String,Object> params=new HashMap<>();
        params.put("USER_NAME",user.name()); params.put("PERIOD",period.toString());
        params.put("REPORT_TITLE",switch(type) { case "categories" -> "Gastos por categoría"; case "summary" -> "Resumen financiero"; default -> "Movimientos del mes"; });
        params.put("INCOME",income); params.put("EXPENSE",expense); params.put("BALANCE",income.subtract(expense));
        params.put("ISSUED",LocalDate.now(ZoneId.of("America/Lima")).toString());
        JasperPrint print=JasperFillManager.fillReport(template(),params,new JRMapCollectionDataSource(data));
        return JasperExportManager.exportReportToPdf(print);
    }
}

package pe.brujula.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;

public final class Models {
    private Models() {}
    public record User(long id, String name, String email, String passwordHash, String role, boolean enabled) {}
    public record AdminUser(long id, String name, String email, String role, boolean enabled) {}
    public record AccountStatus(@NotNull Boolean enabled) {}
    public record Category(long id, String name, String kind) {}
    public record Movement(long id, long categoryId, String category, String kind, String description,
                           BigDecimal amount, LocalDate date, String paymentMethod) {}
    public record Budget(long id, long categoryId, String category, String month, BigDecimal amount) {}
    public record Goal(long id, String name, BigDecimal target, BigDecimal saved, LocalDate deadline) {}
    public record Register(@NotBlank @Size(max=80) String name,
                           @NotBlank @Email @Size(max=160) String email,
                           @NotBlank @Size(min=10,max=64) String password) {}
    public record CategoryInput(@NotBlank @Size(max=60) String name,
                                @Pattern(regexp="INGRESO|GASTO") @NotNull String kind) {}
    public record MovementInput(@Positive long categoryId,
                                @NotBlank @Size(max=160) String description,
                                @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,
                                @NotNull @PastOrPresent LocalDate date,
                                @NotNull @Pattern(regexp="Efectivo|Tarjeta|Yape|Plin|Transferencia") String paymentMethod) {}
    public record BudgetInput(@Positive long categoryId,
                              @NotNull @Pattern(regexp="[0-9]{4}-[0-9]{2}") String month,
                              @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount) {}
    public record GoalInput(@NotBlank @Size(max=80) String name,
                            @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal target,
                            @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal saved,
                            @NotNull LocalDate deadline) {}
}

package com.bankcore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload to withdraw money from an account")
public class WithdrawRequest {

    @NotBlank(message = "Account number is required")
    @Schema(example = "AC1000000001")
    private String accountNumber;

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "0.01", message = "Withdrawal amount must be strictly greater than 0")
    @Schema(example = "500.00")
    private BigDecimal amount;

    @Schema(example = "ATM cash withdrawal")
    private String description;

    @NotBlank(message = "Initiator is required for audit trail")
    @Schema(example = "atm_terminal_pune_04")
    private String initiatedBy;
}

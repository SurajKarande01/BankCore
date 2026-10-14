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
@Schema(description = "Payload to deposit money into an account")
public class DepositRequest {

    @NotBlank(message = "Account number is required")
    @Schema(example = "AC1000000001")
    private String accountNumber;

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "0.01", message = "Deposit amount must be strictly greater than 0")
    @Schema(example = "2000.00")
    private BigDecimal amount;

    @Schema(example = "Cash deposit at branch")
    private String description;

    @NotBlank(message = "Initiator is required for audit trail")
    @Schema(example = "teller_desk_01")
    private String initiatedBy;
}

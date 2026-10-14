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
@Schema(description = "Payload to transfer funds between two accounts")
public class FundTransferRequest {

    @NotBlank(message = "Source account number is required")
    @Schema(example = "AC1000000001", description = "Sender bank account number")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    @Schema(example = "AC1000000002", description = "Recipient bank account number")
    private String destinationAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be strictly greater than 0")
    @Schema(example = "1500.00", description = "Amount to transfer in account currency")
    private BigDecimal amount;

    @Schema(example = "Monthly rent payment", description = "Transaction memo or description")
    private String description;

    @NotBlank(message = "Initiator identity is required for audit trail")
    @Schema(example = "suraj_web_user", description = "User, teller or service triggering this transfer")
    private String initiatedBy;
}

package com.bankcore.dto.request;

import com.bankcore.entity.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload to open a new bank account")
public class CreateAccountRequest {

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Schema(example = "Suraj Karande", description = "Full legal name of the account holder")
    private String accountHolderName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email address must be provided")
    @Schema(example = "suraj@bankcore.com", description = "Customer primary email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+0-9]{10,15}$", message = "Phone number must be valid digits between 10 and 15 digits")
    @Schema(example = "+919876543210", description = "Contact mobile number")
    private String phoneNumber;

    @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
    @Schema(example = "SAVINGS", description = "Type of account")
    private AccountType accountType;

    @NotNull(message = "Initial deposit amount is required")
    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    @Schema(example = "5000.00", description = "Initial opening balance deposit")
    private BigDecimal initialDeposit;

    @Schema(example = "suraj_admin", description = "Identifier of person or system opening the account")
    private String createdBy;
}

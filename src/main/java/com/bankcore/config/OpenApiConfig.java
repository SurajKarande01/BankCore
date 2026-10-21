package com.bankcore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankCore API -- Banking Transaction & Account Ledger Engine")
                        .description("Backend banking transaction engine with ACID transfers, pessimistic locking, audit trail, and statements.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Suraj Daryappa Karande")
                                .email("surajdkarande6396@gmail.com")
                                .url("https://github.com/SurajKarande01"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}


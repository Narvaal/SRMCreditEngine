package com.srmasset.creditengine.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock como bean injetável — nunca {@code LocalDate.now()}/{@code Instant.now()} direto no código
 * de negócio, pra manter cálculos de data determinísticos e testáveis.
 */
@Configuration
public class ClockConfig {

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}

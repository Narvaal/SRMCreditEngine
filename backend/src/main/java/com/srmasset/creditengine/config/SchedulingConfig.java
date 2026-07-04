package com.srmasset.creditengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita a infraestrutura de {@code @Scheduled}. Ligar/desligar cada job é responsabilidade da
 * propriedade do próprio job (ex.: {@code fx-provider.sync.enabled}), não desta config.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}

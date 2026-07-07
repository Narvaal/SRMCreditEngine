plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.0.4"
}

group = "com.srmasset"
// Acompanha a tag semântica da release corrente (SemVer via tags de Git).
version = "1.6.0"
description = "SRM Credit Engine — motor de precificação e liquidação de recebíveis multimoedas"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val springdocOpenapiVersion = "3.0.3"

dependencies {
    // --- API / Web ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapiVersion")

    // --- Precificação ---
    // BigDecimal.pow() nativo só aceita expoente inteiro; o prazo (meses) é fracionário.
    // big-math resolve potência fracionária inteiramente em BigDecimal (via exp/ln), sem cair pra double.
    implementation("ch.obermuhlner:big-math:2.3.2")

    // --- Persistência ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- Observabilidade (Actuator expõe /actuator/prometheus; Grafana consome via Prometheus) ---
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // --- Resiliência (retry + circuit breaker na integração com o provider de taxas) ---
    // starter-aop é pré-requisito das anotações @Retry/@CircuitBreaker (proxy AspectJ).
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // --- Boilerplate ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- Testes ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events("failed")
    }
}

// Cobertura medida, não estimada: relatório gerado junto com `test` e publicado
// como artifact no CI. Sem gate de % — o número serve pra conversa, não pra quebrar build.
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        target("src/**/*.java")
    }
    kotlinGradle {
        ktlint()
        target("*.gradle.kts")
    }
}

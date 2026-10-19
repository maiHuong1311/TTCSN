package com.learnflow.backend;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.MySQLContainer;

/** The datasource can be overridden with -Dspring.datasource.* to run against local MySQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
public abstract class AbstractIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    @Profile("container-test")
    static class ContainerConfig {

        private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

        @Bean
        @ServiceConnection
        MySQLContainer<?> mysqlContainer() {
            return MYSQL;
        }
    }
}

package nl.hackyourfuture.project.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    // CI starts an empty database; create the data team's source table before Flyway builds event_feed.
    return new PostgreSQLContainer(DockerImageName.parse("postgres:18.4-alpine"))
        .withInitScript("db/test-init.sql");
  }

}

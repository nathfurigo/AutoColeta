package com.tecnolog.autocoleta.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class DataSourceConfig {

  // ---- POSTGRES (PRIMÁRIO) ----
  @Primary
  @Bean(name = "postgresDataSource")
  @ConfigurationProperties(prefix = "spring.datasource")  // <<-- usa spring.datasource.*
  public DataSource postgresDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Primary
  @Bean(name = "postgresJdbcTemplate")
  public JdbcTemplate postgresJdbcTemplate(@Qualifier("postgresDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Primary
  @Bean(name = "transactionManager")
  public DataSourceTransactionManager postgresTxManager(@Qualifier("postgresDataSource") DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }

  // ---- SQL SERVER (SECUNDÁRIO) ----
  @Bean(name = "sqlServerDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.sqlserver")
  public DataSource sqlServerDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "sqlServerJdbcTemplate")
  public JdbcTemplate sqlServerJdbcTemplate(@Qualifier("sqlServerDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }
}

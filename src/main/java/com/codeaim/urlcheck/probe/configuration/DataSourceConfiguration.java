package com.codeaim.urlcheck.probe.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Profile("production")
public class DataSourceConfiguration {

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.dataSourceClassName}")
    private String dataSourceClassName;

    @Bean
    public DataSource primaryDataSource()
    {
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.put("url", dataSourceUrl);

        Properties hikariProperties = new Properties();
        hikariProperties.put("dataSourceClassName", dataSourceClassName);
        hikariProperties.put("dataSourceProperties", dataSourceProperties);

        HikariConfig hikariConfig = new HikariConfig(hikariProperties);
        return new HikariDataSource(hikariConfig);
    }
}

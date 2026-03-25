package io.github.jason07289.cicd.mcp;

import io.github.jason07289.cicd.mcp.config.CicdMcpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CicdMcpProperties.class)
public class CicdMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CicdMcpApplication.class, args);
    }
}

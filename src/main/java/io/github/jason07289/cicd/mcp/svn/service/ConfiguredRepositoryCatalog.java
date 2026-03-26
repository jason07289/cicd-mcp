package io.github.jason07289.cicd.mcp.svn.service;

import io.github.jason07289.cicd.mcp.config.CicdMcpProperties;
import io.github.jason07289.cicd.mcp.svn.api.RepositoryCatalog;
import io.github.jason07289.cicd.mcp.svn.api.RepositorySummary;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConfiguredRepositoryCatalog implements RepositoryCatalog {

    private final CicdMcpProperties properties;

    public ConfiguredRepositoryCatalog(CicdMcpProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<RepositorySummary> listRepositories() {
        return properties.getRepositories().stream()
                .map(
                        r ->
                                RepositorySummary.fromConfig(
                                        r.getId(), r.getName(), r.getRootUrl(), r.getGroup()))
                .toList();
    }
}

package io.github.jason07289.cicd.mcp.svn.service;

import io.github.jason07289.cicd.mcp.config.CicdMcpProperties;
import io.github.jason07289.cicd.mcp.config.CicdMcpProperties.RepositoryEntry;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;

@Component
public class RepositoryEntryResolver {

    private final CicdMcpProperties properties;

    public RepositoryEntryResolver(CicdMcpProperties properties) {
        this.properties = properties;
    }

    public RepositoryEntry require(String repositoryId) {
        if (repositoryId == null || repositoryId.isBlank()) {
            throw new NoSuchElementException("repository_id is required");
        }
        return properties.getRepositories().stream()
                .filter(r -> repositoryId.equals(r.getId()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new NoSuchElementException(
                                        "Unknown repository_id: " + repositoryId));
    }
}

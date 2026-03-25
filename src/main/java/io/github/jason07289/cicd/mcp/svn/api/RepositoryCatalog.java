package io.github.jason07289.cicd.mcp.svn.api;

import java.util.List;

/** Read-only view of configured repositories (no credentials). */
public interface RepositoryCatalog {

    List<RepositorySummary> listRepositories();
}

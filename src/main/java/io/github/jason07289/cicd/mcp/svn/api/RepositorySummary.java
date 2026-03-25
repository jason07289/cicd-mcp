package io.github.jason07289.cicd.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositorySummary(String id, String name, String rootUrl, String group) {}

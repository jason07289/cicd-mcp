package io.github.jason07289.cicd.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiffRevisionResult(
        String path,
        long revision,
        long fromRevision,
        String unifiedDiff,
        boolean truncated) {}

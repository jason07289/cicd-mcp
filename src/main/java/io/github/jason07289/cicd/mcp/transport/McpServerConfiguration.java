package io.github.jason07289.cicd.mcp.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jason07289.cicd.mcp.svn.api.RepositoryCatalog;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class McpServerConfiguration {

    private static final McpSchema.JsonSchema EMPTY_OBJECT_SCHEMA =
            new McpSchema.JsonSchema("object", Map.of(), List.of(), true, null, null);

    @Bean
    WebMvcStreamableServerTransportProvider mcpStreamableTransport() {
        return WebMvcStreamableServerTransportProvider.builder().mcpEndpoint("/mcp").build();
    }

    @Bean
    McpSyncServer mcpSyncServer(
            WebMvcStreamableServerTransportProvider transport,
            RepositoryCatalog repositoryCatalog,
            ObjectMapper objectMapper) {
        return McpServer.sync(transport)
                .serverInfo("jason07289-cicd-mcp", "0.1.0")
                .capabilities(
                        McpSchema.ServerCapabilities.builder().tools(false).build())
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("list_repositories")
                                .description(
                                        "Lists SVN repositories configured in application.yml (id, name, root URL, group).")
                                .inputSchema(EMPTY_OBJECT_SCHEMA)
                                .build(),
                        (exchange, request) -> {
                            try {
                                String json =
                                        objectMapper.writeValueAsString(
                                                repositoryCatalog.listRepositories());
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(json)))
                                        .isError(false)
                                        .build();
                            } catch (JsonProcessingException e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(
                                                List.of(
                                                        new McpSchema.TextContent(
                                                                "Serialization error: "
                                                                        + e.getOriginalMessage())))
                                        .isError(true)
                                        .build();
                            }
                        })
                .build();
    }

    @Bean
    @DependsOn("mcpSyncServer")
    RouterFunction<ServerResponse> mcpRouterFunction(
            WebMvcStreamableServerTransportProvider transport) {
        return transport.getRouterFunction();
    }
}

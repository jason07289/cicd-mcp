package io.github.jason07289.cicd.mcp.svn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jason07289.cicd.mcp.config.CicdMcpProperties;
import io.github.jason07289.cicd.mcp.svn.api.RepositorySummary;
import io.github.jason07289.cicd.mcp.svn.api.SvnAccessException;
import io.github.jason07289.cicd.mcp.svn.svnkit.LocalSvnRepositorySupport;
import java.util.List;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("svnkit")
class SvnServerDiscoveryTest {

    @TempDir Path tempDir;

    @Test
    void discover_listsImmediateChildDirectories() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createRepoWithTwoRootDirectories(tempDir);
        SvnServerDiscovery discovery = discovery(fx.properties());

        List<RepositorySummary> found =
                discovery.discover(fx.repoUrl().toString(), null, null, null);

        assertThat(found)
                .extracting(RepositorySummary::name)
                .containsExactlyInAnyOrder("a", "b");
        assertThat(found)
                .allSatisfy(
                        s -> {
                            assertThat(s.source()).isEqualTo("discovered");
                            assertThat(s.discoveredUnder()).isEqualTo(fx.repoUrl().toString());
                            assertThat(s.id()).startsWith("discovered:");
                        });
        assertThat(
                        found.stream()
                                .filter(s -> "a".equals(s.name()))
                                .findFirst()
                                .orElseThrow()
                                .rootUrl())
                .contains("a");
        assertThat(
                        found.stream()
                                .filter(s -> "b".equals(s.name()))
                                .findFirst()
                                .orElseThrow()
                                .rootUrl())
                .contains("b");
    }

    @Test
    void discover_withCredentialRepositoryId_usesConfiguredCredentials() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createRepoWithTwoRootDirectories(tempDir);
        SvnServerDiscovery discovery = discovery(fx.properties());

        List<RepositorySummary> found =
                discovery.discover(fx.repoUrl().toString(), "local", null, null);

        assertThat(found).hasSize(2);
    }

    @Test
    void discover_unknownCredentialRepositoryId_throws() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createRepoWithTwoRootDirectories(tempDir);
        SvnServerDiscovery discovery = discovery(fx.properties());

        assertThatThrownBy(
                        () ->
                                discovery.discover(
                                        fx.repoUrl().toString(), "missing-id", null, null))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void discover_invalidUrl_throwsSvnAccessException() {
        CicdMcpProperties props = new CicdMcpProperties();
        SvnServerDiscovery discovery = discovery(props);

        assertThatThrownBy(() -> discovery.discover("not-a-valid-svn-url:", null, null, null))
                .isInstanceOf(SvnAccessException.class)
                .hasMessageContaining("Invalid server_url");
    }

    private static SvnServerDiscovery discovery(CicdMcpProperties properties) {
        RepositoryEntryResolver resolver = new RepositoryEntryResolver(properties);
        return new SvnServerDiscovery(resolver);
    }
}

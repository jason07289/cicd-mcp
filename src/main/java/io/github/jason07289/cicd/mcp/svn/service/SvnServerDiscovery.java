package io.github.jason07289.cicd.mcp.svn.service;

import io.github.jason07289.cicd.mcp.config.CicdMcpProperties.RepositoryEntry;
import io.github.jason07289.cicd.mcp.svn.api.RepositorySummary;
import io.github.jason07289.cicd.mcp.svn.api.SvnAccessException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Lists immediate child <strong>directories</strong> at a parent SVN URL (same idea as {@code svn
 * list}). On multi-repository layouts (e.g. svnserve with multiple repos under one root), each
 * child name is a repository. On a single-repo root, results may be {@code trunk}, {@code
 * branches}, etc.—callers must interpret context.
 */
@Service
public class SvnServerDiscovery {

    private static final Logger log = LoggerFactory.getLogger(SvnServerDiscovery.class);

    private final RepositoryEntryResolver resolver;

    public SvnServerDiscovery(RepositoryEntryResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * @param serverUrl parent URL (e.g. {@code svn://host:3690/})
     * @param credentialRepositoryId if non-blank, use credentials from this configured repository
     * @param username optional explicit username (used when {@code credentialRepositoryId} is
     *     blank)
     * @param password optional explicit password (used when {@code credentialRepositoryId} is
     *     blank)
     */
    public List<RepositorySummary> discover(
            String serverUrl,
            String credentialRepositoryId,
            String username,
            String password)
            throws SvnAccessException {
        String normalized = normalizeServerUrl(serverUrl);
        SVNURL parent;
        try {
            parent = SVNURL.parseURIEncoded(normalized);
        } catch (SVNException e) {
            throw new SvnAccessException("Invalid server_url: " + svnMessage(e), e);
        }

        ISVNAuthenticationManager auth = buildAuth(credentialRepositoryId, username, password);

        SVNRepository repository;
        try {
            repository = SVNRepositoryFactory.create(parent);
        } catch (SVNException e) {
            throw new SvnAccessException("Invalid server_url: " + svnMessage(e), e);
        }
        repository.setAuthenticationManager(auth);
        try {
            long rev = repository.getLatestRevision();
            List<RepositorySummary> out = new ArrayList<>();
            repository.getDir(
                    "",
                    rev,
                    null,
                    (SVNDirEntry entry) -> {
                        if (entry.getKind() != SVNNodeKind.DIR) {
                            return;
                        }
                        String name = entry.getName();
                        try {
                            SVNURL child = parent.appendPath(name, false);
                            String id = "discovered:" + sanitizeForId(name);
                            out.add(
                                    new RepositorySummary(
                                            id,
                                            name,
                                            child.toString(),
                                            "discovered",
                                            "discovered",
                                            parent.toString()));
                        } catch (SVNException e) {
                            log.warn(
                                    "Skipping discovered entry {} under {}: {}",
                                    name,
                                    parent,
                                    svnMessage(e));
                        }
                    });
            out.sort(Comparator.comparing(RepositorySummary::name, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(out);
        } catch (SVNException e) {
            log.error("SVN discovery failed for parent {}: {}", parent, svnMessage(e), e);
            throw new SvnAccessException(svnMessage(e), e);
        } finally {
            repository.closeSession();
        }
    }

    static String normalizeServerUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return s;
        }
        return s;
    }

    private ISVNAuthenticationManager buildAuth(
            String credentialRepositoryId, String username, String password) {
        if (credentialRepositoryId != null && !credentialRepositoryId.isBlank()) {
            RepositoryEntry entry = resolver.require(credentialRepositoryId);
            return buildAuthFromEntry(entry);
        }
        boolean hasUser = username != null && !username.isBlank();
        boolean hasPass = password != null && !password.isBlank();
        if (hasUser || hasPass) {
            return SVNWCUtil.createDefaultAuthenticationManager(
                    hasUser ? username.trim() : "", hasPass ? password.toCharArray() : null);
        }
        return SVNWCUtil.createDefaultAuthenticationManager();
    }

    private static ISVNAuthenticationManager buildAuthFromEntry(RepositoryEntry entry) {
        String user = entry.getCredentials().getUsername();
        String pass = entry.getCredentials().getPassword();
        boolean hasUser = user != null && !user.isBlank();
        boolean hasPass = pass != null && !pass.isBlank();
        if (hasUser || hasPass) {
            return SVNWCUtil.createDefaultAuthenticationManager(
                    hasUser ? user : "", hasPass ? pass.toCharArray() : null);
        }
        return SVNWCUtil.createDefaultAuthenticationManager();
    }

    private static String sanitizeForId(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String s = sb.toString();
        return s.isEmpty() ? "unnamed" : s;
    }

    private static String svnMessage(SVNException e) {
        String m = e.getMessage();
        return m != null ? m : e.getClass().getSimpleName();
    }
}

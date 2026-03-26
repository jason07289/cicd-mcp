# SVN MCP Server

**SVN MCP** is a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that exposes **Subversion (SVN)** repositories to AI coding assistants (for example Cursor or Claude Desktop). Clients connect over **Streamable HTTP**; the server talks to SVN with **SVNKit** inside the JVM—**no `svn` CLI** is required on the client or IDE host.

> *WebSVN-style read workflows, callable as MCP tools over Streamable HTTP.*

[한국어 문서](README_kr.md) · Full requirements: [PRD.md](PRD.md) · Code layout: [ARCHITECTURE.md](ARCHITECTURE.md)

---

## Why SVN MCP?

Traditional SVN browsing (for example [WebSVN](https://github.com/websvnphp/websvn)) assumes a browser. **SVN MCP** brings the same *kinds* of operations into the MCP ecosystem so agents can list repos, browse paths, read files, inspect history, diff, and search—using a single MCP connection instead of shelling out to `svn`.

---

## Features (from PRD)

Capabilities are aligned with WebSVN-style flows and exposed as MCP **`tool`** calls (names are illustrative; implementation uses consistent `snake_case`).

| Area | Tools / behavior |
|------|------------------|
| **Repositories** | `list_repositories` — configured repo IDs, names, root URLs, groups; optional last-activity style info (`repo_last_activity`). |
| **Browse** | `list_path` — directory entries at a revision (or peg); tree or flat view with limits. |
| **Files** | `get_file` — file contents at a revision (text or Base64); metadata such as `mime_type`, `is_text`, `encoding_hint`. |
| **History** | `get_log` — commit log for a path with limits and revision range; `changed_paths` per revision; optional Bugtraq issue extraction from log messages. |
| **Revision** | `get_revision` — one revision’s metadata and changed paths (add/modify/delete). |
| **Diff** | `diff_file` — compare two revisions of a file (e.g. unified diff); `ignore_whitespace` option. `diff_paths` — diff across paths/revisions. |
| **Blame** | `blame_file` — per-line revision, author, and content. |
| **Search** | `search` — query within repo/path/revision bounds with documented performance limits. |
| **Activity** | `get_recent_activity` — recent changes as JSON (RSS-style feed behavior without requiring public RSS hosting). |
| **Export** | `export_path` — export archive or streaming download (with size/security policies). |
| **Security** | Optional **`authz`**-based read guards on paths; credentials only on the server (env / secrets). |

**In scope:** MCP protocol, Streamable HTTP transport, read-heavy SVN operations above, multi-repo config, optional path-level authz.

**Out of scope (for now):** Recreating WebSVN’s PHP UI; **write** operations (commit, propset, …); hosting a public RSS feed (agents use `get_recent_activity` instead).

---

## Stack

| Layer | Choice |
|-------|--------|
| Runtime | Java **17** |
| App framework | **Spring Boot** |
| MCP transport | **Streamable HTTP** (default for AI tools) |
| SVN access | **SVNKit** (pure Java; protocols per SVNKit support) |

---

## Roadmap (high level)

| Phase | Focus |
|-------|--------|
| **MVP** | `list_repositories`, `list_path`, `get_file`, `get_log`, `get_revision`, `diff_file`, `blame_file`, basic errors |
| **1.1** | `diff_paths`, limited `search`, authz guard, tighter response limits |
| **1.2** | `export_path`, Bugtraq extraction, `get_recent_activity` |
| **2.0** | Optional syntax highlight, RSS XML, advanced search (TBD) |

---

## Build and run

```bash
./gradlew build
./gradlew bootRun
```

- **MCP endpoint (Streamable HTTP):** `http://localhost:8080/mcp` (default port **8080**).
- **Configuration:** `src/main/resources/application.yml`, prefix `io.github.jason07289.cicd.mcp` (nested under `io.github.jason07289.cicd.mcp` in YAML). Demo credentials can be overridden with `SVN_DEMO_USER` and `SVN_DEMO_PASSWORD`.
- **Implemented today (MVP tools):** `list_repositories` (config only), `list_path`, `get_file`, `get_log`, `get_revision`, `diff_file`, `blame_file` (SVNKit against each repo `root_url`; credentials stay server-side and are never returned in tool output).

---

## Package name

Application base package: **`io.github.jason07289.cicd.mcp`**.

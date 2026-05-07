# SVN MCP 서버

**SVN MCP**는 **Subversion(SVN)** 저장소를 AI 코딩 어시스턴트(예: Cursor, Claude Desktop)가 쓸 수 있도록 [Model Context Protocol(MCP)](https://modelcontextprotocol.io/)로 노출하는 서버입니다. 클라이언트는 **Streamable HTTP**로 연결하고, SVN 접근은 JVM 안의 **SVNKit**으로 처리합니다. 클라이언트·IDE 호스트에는 **`svn` CLI 설치가 필요 없습니다.**

> *WebSVN이 브라우저에 주던 읽기 흐름을, Streamable HTTP로 호출 가능한 MCP 도구로 제공합니다.*

[English README](README.md) · 상세 요구: [PRD.md](PRD.md) · 코드 구조: [ARCHITECTURE.md](ARCHITECTURE.md) · 시나리오: [기간별 생산량](docs/scenarios/productivity-by-period.md)

---

## SVN MCP를 쓰는 이유

기존 SVN 탐색([WebSVN](https://github.com/websvnphp/websvn) 등)은 브라우저 전제에 가깝습니다. **SVN MCP**는 같은 *종류*의 조회·비교·검색을 MCP 생태계로 가져와, 에이전트가 저장소 목록·경로·파일·이력·diff·검색 등을 **MCP 연결 하나**로 다루게 합니다.

---

## 기능 요약 (현재 구현 MCP 도구)

아래는 `McpServerConfiguration`에 등록된 **`tool`** 이름과 JSON 스키마상 **필수(`required`) 파라미터**입니다. 생략 가능한 인자는 각 도구 설명에 따릅니다.

| 영역 | 도구 | 필수 파라미터 | 동작 요약 |
|------|------|---------------|-----------|
| **저장소** | `list_repositories` | 없음 | 설정된 저장소 ID·이름·루트 URL·그룹 등(자격 증명 미포함). |
| **탐색** | `list_path` | `repository_id` | 리비전에서 디렉터리 한 단계(`view_mode=tree`) 또는 경로 하위 파일 나열(`flat`); 선택: `path`(기본 루트), `revision`, `peg_revision`, `view_mode`, `flat_max_depth`, `flat_max_entries`. |
| **파일** | `get_file` | `repository_id`, `path` | 파일 내용(텍스트 또는 Base64), MIME·텍스트 여부 등; 선택: `revision`, `peg_revision`. |
| **로그** | `get_log` | `repository_id` | 경로에 영향 준 커밋 로그; 선택: `path`, `limit`, `start_revision`/`end_revision`, `stop_on_copy`, `start_date`/`end_date`(ISO-8601), `author`, `author_match`(`exact`\|`contains`). |
| **리비전** | `get_revision` | `repository_id`, `revision` | 단일 리비전 메타데이터·변경 경로. |
| **Diff (파일)** | `diff_file` | `repository_id`, `path`, `from_revision`, `to_revision` | 두 리비전 간 unified diff; 선택: `ignore_whitespace`. |
| **Diff (리비전)** | `diff_revision` | `repository_id`, `revision` | `svn diff -c REV`에 해당; 선택: `path`(접두), `ignore_whitespace`. |
| **Blame** | `blame_file` | `repository_id`, `path` | 줄 단위 blame; 선택: `revision`(annotate 상한, 생략 시 HEAD). |
| **기간→리비전** | `resolve_revision_range` | `repository_id`, `start_inclusive`, `end_inclusive` | 시간 구간을 리비전 범위로 근사 매핑; 선택: `path`. |
| **통계** | `repository_author_stats` | `repository_id` | 기간별 작성자 커밋·diff 라인 집계. 시간 창은 **`calendar_date`**(+선택 `timezone`) **또는** **`start_inclusive`·`end_inclusive`**(ISO-8601) 중 하나로 지정; 선택: `path_prefix`, `max_revisions_to_analyze`. |

PRD·로드맵에만 있는 **`search`**, **`get_recent_activity`**, **`export_path`**, **`diff_paths`**, 선택 **`authz`** 등은 아직 MCP 도구로 노출되지 않습니다. 자격 증명은 서버 설정(env·시크릿)에만 두고 응답에 포함하지 않습니다.

**포함:** MCP 프로토콜, Streamable HTTP, 위 읽기 중심 SVN 작업, 다중 저장소, 선택적 경로 권한.

**제외·후순위:** WebSVN PHP UI 재현, **쓰기**(commit 등), 외부 공개 RSS 피드 호스팅(에이전트는 `get_recent_activity` 등으로 대체).

---

## 기술 스택

| 구분 | 선택 |
|------|------|
| 런타임 | Java **17** |
| 앱 | **Spring Boot** |
| MCP 전송 | **Streamable HTTP** |
| SVN | **SVNKit**(순수 Java, 프로토콜은 SVNKit 지원 범위) |

---

## 로드맵 (요약)

| 단계 | 내용 |
|------|------|
| **MVP** | `list_repositories`, `list_path`, `get_file`, `get_log`, `get_revision`, `diff_file`, `blame_file`, 기본 오류 |
| **1.1** | `diff_paths`, 제한적 `search`, authz 가드, 응답 한도 강화 |
| **1.2** | `export_path`, Bugtraq 추출, `get_recent_activity` |
| **2.0** | 선택 구문 하이라이트, RSS XML, 고급 검색(검토) |

---

## 빌드·실행

```bash
./gradlew build
./gradlew bootRun
```

- **MCP 엔드포인트(Streamable HTTP):** `http://localhost:8765/mcp` (기본 포트 **8765**, `application.yml`의 `server.port`).

### Docker

저장소 루트에서 이미지를 빌드하고 컨테이너를 띄웁니다. 호스트·컨테이너 모두 **8765**를 사용합니다.

```bash
docker compose up --build
```

- **MCP URL:** `http://localhost:8765/mcp`
- 포트를 바꾸려면 `docker-compose.yml`의 `ports`를 `"원하는호스트포트:8765"`처럼 조정하고, 컨테이너 쪽 앱 포트는 `SERVER_PORT` 또는 `application.yml`로 맞춥니다.
- **설정:** `src/main/resources/application.yml`, 프로퍼티 접두사 `io.github.jason07289.cicd.mcp`(YAML에서는 `io` → `github` → `jason07289` → … 중첩). 데모 자격 증명은 `SVN_DEMO_USER`, `SVN_DEMO_PASSWORD`로 덮어쓸 수 있습니다.
- **현재 구현:** 상단 기능 요약 표에 나열한 MCP 도구(SVNKit 기반 읽기 전용).

---

## 패키지

애플리케이션 기본 패키지: **`io.github.jason07289.cicd.mcp`**.

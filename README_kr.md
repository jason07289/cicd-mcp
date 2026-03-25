# SVN MCP 서버

**SVN MCP**는 **Subversion(SVN)** 저장소를 AI 코딩 어시스턴트(예: Cursor, Claude Desktop)가 쓸 수 있도록 [Model Context Protocol(MCP)](https://modelcontextprotocol.io/)로 노출하는 서버입니다. 클라이언트는 **Streamable HTTP**로 연결하고, SVN 접근은 JVM 안의 **SVNKit**으로 처리합니다. 클라이언트·IDE 호스트에는 **`svn` CLI 설치가 필요 없습니다.**

> *WebSVN이 브라우저에 주던 읽기 흐름을, Streamable HTTP로 호출 가능한 MCP 도구로 제공합니다.*

[English README](README.md) · 상세 요구: [PRD.md](PRD.md) · 코드 구조: [ARCHITECTURE.md](ARCHITECTURE.md)

---

## SVN MCP를 쓰는 이유

기존 SVN 탐색([WebSVN](https://github.com/websvnphp/websvn) 등)은 브라우저 전제에 가깝습니다. **SVN MCP**는 같은 *종류*의 조회·비교·검색을 MCP 생태계로 가져와, 에이전트가 저장소 목록·경로·파일·이력·diff·검색 등을 **MCP 연결 하나**로 다루게 합니다.

---

## 기능 요약 (PRD 기준)

WebSVN 기능 축에 맞춰 MCP **`tool`**로 노출합니다(이름은 예시이며 구현 시 `snake_case`로 통일).

| 영역 | 도구 / 동작 |
|------|-------------|
| **저장소** | `list_repositories` — 설정된 저장소 ID·이름·루트 URL·그룹; 선택적으로 최근 활동(`repo_last_activity`). |
| **탐색** | `list_path` — 특정 리비전(또는 peg)에서 디렉터리 자식; 트리/플랫 뷰 및 한도. |
| **파일** | `get_file` — 리비전별 파일 내용(텍스트 또는 Base64); `mime_type`, `is_text`, `encoding_hint` 등. |
| **로그** | `get_log` — 경로별 커밋 로그, 리비전 범위·한도; 리비전별 `changed_paths`; Bugtraq 이슈 ID 추출(설정 시). |
| **리비전** | `get_revision` — 한 리비전의 메타데이터와 변경 경로(추가/수정/삭제). |
| **Diff** | `diff_file` — 인접 리비전 파일 비교(unified diff 등); `ignore_whitespace`. `diff_paths` — 경로/리비전 간 비교. |
| **Blame** | `blame_file` — 줄별 리비전·작성자·내용. |
| **검색** | `search` — 저장소/경로/리비전 범위 검색(성능·부하 정책 문서화). |
| **활동** | `get_recent_activity` — 최근 변경 JSON 요약(RSS 공개 호스팅 대체). |
| **내보내기** | `export_path` — export 아카이브 또는 스트리밍(용량·보안 정책). |
| **보안** | 선택적 **`authz`** 경로 읽기 가드; 자격 증명은 서버(env·시크릿)에만. |

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

- **MCP 엔드포인트(Streamable HTTP):** `http://localhost:8080/mcp` (기본 포트 **8080**).
- **설정:** `src/main/resources/application.yml`, 프로퍼티 접두사 `io.github.jason07289.cicd.mcp`(YAML에서는 `io` → `github` → `jason07289` → … 중첩). 데모 자격 증명은 `SVN_DEMO_USER`, `SVN_DEMO_PASSWORD`로 덮어쓸 수 있습니다.
- **현재 구현:** `list_repositories`만(설정 기반 메타데이터; 비밀번호는 응답에 포함하지 않음).

---

## 패키지

애플리케이션 기본 패키지: **`io.github.jason07289.cicd.mcp`**.

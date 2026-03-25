# PRD: SVN MCP (Spring Boot / Java 17)

## 1. 개요

### 1.1 목적

Subversion(SVN) 저장소를 Model Context Protocol(MCP) 클라이언트(예: Cursor, Claude Desktop)에서 조회·비교·검색할 수 있도록, **Java 17 및 Spring Boot 기반 MCP 서버**를 제공한다. **AI 코딩 도구와의 MCP 연결 방식은 Streamable HTTP**를 사용한다. UI는 PHP 기반 [WebSVN](https://github.com/websvnphp/websvn)과 동일한 **기능 축**을 따르되, MCP `tool`로 노출한다.

### 1.2 참고 자료

| 항목 | 위치 |
|------|------|
| 업스트림 WebSVN | https://github.com/websvnphp/websvn |
| 로컬 소스 (기능·흐름 참고) | `/Users/user/workspace/ai/mcp/websvn` |

WebSVN의 각 PHP 엔드포인트는 **동작 의미(어떤 svn 명령·옵션에 대응하는지)** 를 파악하는 레퍼런스로 사용한다. 구현은 **PHP를 재사용하지 않고** Spring Boot 애플리케이션과 **MCP 서버 프로세스 내부의 SVN 접근 계층**으로 수행한다. SVN 저장소와의 연동은 **SVNKit**(Subversion용 순수 Java 오픈 소스 라이브러리)을 사용한다. (접근 계층의 상세는 [3. 아키텍처 원칙](#3-아키텍처-원칙) 참고.)

### 1.3 제품 한 줄 정의

> “WebSVN이 브라우저에 주는 기능을, AI 에이전트가 Streamable HTTP로 호출할 수 있는 MCP 도구로 제공하는 SVN 게이트웨이.”

---

## 2. 범위

### 2.1 포함 (In Scope)

- MCP 서버 프로토콜(도구 등록, 호출, 오류 응답) 준수
- Spring Boot 애플리케이션으로 **MCP Streamable HTTP** 전송(스펙에 맞는 엔드포인트·세션) — AI 코딩 도구와의 **기본·권장** 연결 방식
- 아래 [기능·도구 매핑](#4-기능-요구사항-mcp-도구--websvn-매핑)에 정의된 SVN 작업
- **SVNKit**을 통한 저장소 접근(네이티브 `svn` CLI 의존 없이 JVM 내에서 프로토콜·인증 처리; 지원 범위는 SVNKit·배포 JDK 정책에 따름)
- 다중 저장소 구성, 경로별 읽기 권한(선택적 `svnauthz` 등)

### 2.2 제외 또는 후순위 (선택)

- WebSVN의 PHP 템플릿·스킨·세션 UI 재현
- 서브버전 **쓰기** 작업(commit, propset 등) — 본 PRD는 **읽기 중심**으로 정의(별도 요청 시 확장)
- RSS를 외부 공개 피드로 호스팅하는 것(에이전트용으로는 “최근 변경 요약” 도구로 대체 가능)

---

## 3. 아키텍처 원칙

### 3.1 클라이언트 vs MCP 서버(SVN 접근)

| 구분 | 요구 |
|------|------|
| **AI 코딩 도구(IDE) 호스트** | **Subversion CLI 설치를 요구하지 않는다.** MCP 클라이언트는 Streamable HTTP로 본 MCP 서버만 연결하면 된다. |
| **MCP 서버(본 제품 `svn-mcp`)** | **SVN 저장소 접근은 SVNKit(Java)으로 수행한다.** 배포물(예: 실행 가능 JAR, 컨테이너 이미지)에 **Java 17 런타임과 SVNKit**이 포함되며, 이 MCP 프로세스가 설정된 URL로 SVN 서버에 붙는다. `https://` / `svn://` / `svn+ssh://` 등 프로토콜·인증은 **SVNKit이 지원하는 범위** 내에서 처리한다. |
| **운영 전제** | **사용자도, IDE 호스트도, 별도로 OS에 `svn` 명령을 깔아 두는 것을 요구하지 않는다.** 저장소 클라이언트 역할은 **SVNKit + JVM**으로 충족한다. |

### 3.2 MCP 전송

| 원칙 | 설명 |
|------|------|
| **Streamable HTTP** | AI 코딩 도구와의 MCP 통신은 **Streamable HTTP**로 수행한다. |

### 3.3 기타

| 원칙 | 설명 |
|------|------|
| **동작 의미 정렬** | WebSVN·CLI SVN과 동일한 **의미**의 결과를 내도록 맞출 것; 내부 구현은 SVNKit API 사용. |
| **상태 비저장 우선** | 도구 호출 단위로 저장소 ID·리비전·경로를 인자로 받음(MCP 세션은 전송·인증·레이트리밋용). |
| **대용량 응답 제어** | diff/로그/파일 본문은 크기 상한, 페이지네이션, 요약 모드 제공. |
| **보안** | 저장소 자격 증명은 서버 측 환경 변수 또는 시크릿 저장소; 로그에 비밀번호·토큰 금지. |

---

## 4. 기능 요구사항: MCP 도구 ↔ WebSVN 매핑

아래는 사용자 제공 표를 기준으로 **MCP `tool` 이름(예시)** 과 **역할**을 정리한 것이다. 실제 `tool` 이름은 구현 시 snake_case로 일관되게 정한다.

### 4.1 저장소 목록 (`index.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 설정된 여러 저장소를 그룹/트리로 나열 | `list_repositories` — 설정 파일(또는 env)의 저장소 ID, 표시 이름, 루트 URL, 그룹(옵션) 반환 |
| 마지막 수정 정보 표시 옵션 | `list_repositories` 옵션 또는 `repo_last_activity` — SVNKit으로 최근 1건 로그(또는 동등)로 마지막 리비전·시간·작성자(가능 시) |

### 4.2 디렉터리·파일 목록 (`listing.php`, `browse.php`, `multiviews.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 경로별 트리/플랫 뷰 | `list_path` — 특정 리비전(또는 peg)에서 디렉터리 자식 목록; `view_mode`: `tree` \| `flat`(flat은 깊이/최대 항목 제한과 함께) |
| 리비전·페그 리비전과 함께 탐색 | 모든 경로 관련 도구에 `revision`, `peg_revision`(선택) 파라미터 |

### 4.3 파일 보기 (`filedetails.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 특정 리비전의 파일 내용 | `get_file` — 대상 경로, 리비전; 원문 또는 Base64(바이너리) |
| `svn:mime-type`에 따른 표시 | 응답에 `mime_type`, `is_text`, `encoding_hint` 메타데이터 |
| 구문 강조(GeSHi/Enscript) | MCP에서는 **원문 반환**을 기본으로 하고, 하이라이트는 클라이언트 책임 또는 선택 옵션 `highlight: true` 시 서버가 언어 추정 후 HTML/ANSI 등 **한 형식으로만** 제공(구현 복잡도에 따라 Phase 2) |

### 4.4 변경 로그 (`log.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 경로별 커밋 로그 | `get_log` — `path`, `limit`, `start_revision`, `end_revision`, `stop_on_copy`(옵션) |
| 변경 파일 목록 | 각 리비전 항목에 `changed_paths` 요약(상세 로그·verbose 의미에 맞출 것) |
| Bugtraq 연동 | 로그 메시지 원문 + 선택적으로 `bugtraq` 설정이 있으면 이슈 ID 추출(정규식은 설정 기반) |

### 4.5 리비전 상세 (`revision.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 한 리비전에서 바뀐 경로·링크 | `get_revision` — 리비전 메타데이터 + 변경 경로 목록(추가/수정/삭제) |

### 4.6 파일 diff (`diff.php`, `include/diff_inc.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 인접 리비전 두 버전 비교 | `diff_file` — `path`, `revision`(또는 `from_revision`/`to_revision`); SVNKit으로 두 버전 내용을 얻은 뒤 unified diff 등으로 비교(WebSVN과 동등한 UX 목표) |
| 공백 무시 옵션 | `ignore_whitespace: bool` → diff 생성 시 공백 무시 옵션에 매핑 |

### 4.7 경로 간 비교 (`comp.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 두 경로/리비전 비교 | `diff_paths` — `from_path`/`from_rev`, `to_path`/`to_rev`(서버에서 SVN diff 의미에 맞게 수행) |

### 4.8 Blame (`blame.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| blame(주석) | `blame_file` — 줄별 리비전·작성자·내용(또는 테이블 구조 JSON) |

### 4.9 검색 (`search.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 저장소/경로/리비전 범위 검색 | `search` — 쿼리 문자열, 범위(리비전), 경로 프리픽스, `max_matches`; 구현은 로그 기반 필터·제한된 파일 샘플링 등 **성능·부하를 문서화한 전략**(SVNKit API 사용) |

### 4.10 RSS (`rss.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 피드(저장소별 설정) | `get_recent_activity` — RSS 대신 JSON으로 최근 N건 요약(제목, 링크용 경로, 리비전, 시간); 필요 시 별도 `get_rss_xml`(Phase 2) |

### 4.11 다운로드 (`dl.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| export 후 tar/zip | `export_path` — 서버에서 export(동등 동작) 후 임시 아카이브 또는 **스트리밍 URL**(보안상 일회성 토큰 권장). 에이전트가 대용량 바이너리를 직접 받기 어렵다면 “요약 + 경로 목록만” 모드 제공 |

### 4.12 인증·권한 (`include/authz.php`)

| 요구 | MCP 도구 역할 |
|------|----------------|
| 경로별 읽기 권한 | 모든 도구 공통 **권한 가드**: `authz` 파일 경로 설정 시, 요청 경로가 허용되는지 검사 후 거부 시 MCP 오류 반환 |
| 저장소 접근 자격 증명 | 서버 측 설정(환경 변수·시크릿)으로 저장소별 사용자/비밀번호 또는 클라이언트 인증서 등 **SVNKit이 지원하는 범위**를 명시 |

---

## 5. 비기능 요구사항

| ID | 항목 | 기준 |
|----|------|------|
| NFR-1 | **성능** | 단일 `get_log`/`diff` 호출에 타임아웃(설정 가능); 기본 limit 상한 |
| NFR-2 | **안정성** | SVNKit·네트워크 오류 시 안전한 오류 메시지(내부 스택 노출 최소화) |
| NFR-3 | **관측 가능성** | 구조화 로그(저장소 ID, 도구명, 소요 시간); 비밀번호/토큰 마스킹 |
| NFR-4 | **배포** | Spring Boot 실행 JAR(또는 컨테이너)로 **Streamable HTTP MCP** 엔드포인트 노출; 클라이언트는 별도 `svn` CLI 설치 불필요함을 문서에 명시 |
| NFR-5 | **테스트** | 핵심 도구에 대해 SVNKit mock 또는 샘플 repo로 단위/통합 테스트(권장) |

---

## 6. 설정 모델 (초안)

### 6.1 요구사항

- **Spring Boot `application.yml`**(및 `application-{profile}.yml`)로 **SVN 서버·저장소 기본 정보**를 선언할 수 있어야 한다. 여기에 포함되는 항목(예시)은 다음과 같다.
  - 저장소 **식별자**(`id`), **표시 이름**, **SVN 루트 URL**(`root_url` 등)
  - **접속 자격 증명**(사용자명·비밀번호 등 SVNKit이 지원하는 형태) — 가능하면 **환경 변수·시크릿 참조**로만 주입하고, 평문을 저장소에 커밋하지 않도록 한다.
  - (선택) 그룹, `authz` 파일 경로, Bugtraq 정규식, 응답 한도(`defaults`) 등
- 동일 내용은 **환경 변수**로 덮어쓰거나, 필요 시 **별도 YAML 경로**를 가리키는 방식으로도 로드할 수 있게 한다(Spring Boot 외부 설정 관례).
- **SVN 접근은 SVNKit 전제**이므로 `SVN_BIN` 같은 CLI 경로는 본 제품의 기본 설계에 포함하지 않는다(필요 시 레거시·진단용 옵션으로만 검토).

### 6.2 예시 (`application.yml`)

프리픽스·프로퍼티 이름은 구현 시 `@ConfigurationProperties`와 맞춘다. 아래는 구조만 나타낸 예시다.

```yaml
# application.yml (예시)
io:
  github:
    jason07289:
      cicd:
        mcp:
          repositories:
            - id: myproj
              name: My Project
              root_url: https://svn.example.com/myproj
              group: products
              credentials:
                username: ${SVN_MYPROJ_USER:}
                password: ${SVN_MYPROJ_PASSWORD:}
              bugtraq:
                log_regex: "ISSUE-(\\d+)"
          authz_file: /path/to/authz
          defaults:
            log_limit_max: 500
            file_content_max_bytes: 2_000_000
```

---

## 7. 단계별 로드맵 (제안)

| Phase | 내용 |
|-------|------|
| **MVP** | `list_repositories`, `list_path`, `get_file`, `get_log`, `get_revision`, `diff_file`, `blame_file`, 기본 오류 처리 |
| **1.1** | `diff_paths`, `search`(제한적), `authz` 가드, 응답 크기 제한 강화 |
| **1.2** | `export_path`, Bugtraq 추출, `get_recent_activity` |
| **2.0** | 선택적 구문 하이라이트, RSS XML, 고급 검색(인덱스/외부 도구 연동 검토) |

---

## 8. 성공 지표

- WebSVN 표에 대응하는 **읽기 흐름**을 MCP 도구만으로 재현 가능할 것(에이전트 데모 시나리오).
- **IDE/에이전트 호스트에 Subversion CLI를 설치하지 않아도** Streamable HTTP로 MCP만 연결해 동일 기능을 쓸 수 있을 것.
- MCP 서버가 네트워크·자격 증명이 허용되는 한 **SVN 서버에 직접 접근(SVNKit)** 해 도구가 성공할 것.
- 권한이 없는 경로는 **일관되게 거부**될 것.
- 대용량 diff/로그에서도 **서버·클라이언트가 멈추지 않도록** 기본 한도가 동작할 것.

---

## 9. 용어

| 용어 | 설명 |
|------|------|
| **Peg revision** | 경로 이동(rename) 이력이 있을 때 기준이 되는 리비전 |
| **MCP tool** | MCP 스펙에 따라 노출되는 callable 기능 단위 |
| **Streamable HTTP** | MCP 원격 연결용 전송 방식(본 PRD에서 AI 코딩 도구 연결의 표준) |
| **SVNKit** | Subversion과 연동하기 위한 순수 Java 오픈 소스 라이브러리; 본 제품의 저장소 접근 계층으로 사용 |

---

## 10. 오픈 이슈 (결정 필요)

1. **SVNKit 버전·지원 프로토콜**: `svn+ssh`·클라이언트 인증서 등 운영 환경에서 필요한 연결 방식이 SVNKit·선택한 트랜스포트에서 모두 가능한지 검증.  
2. 바이너리/대용량 파일: Base64 크기 한도 및 “다운로드 URL만 반환” 정책.  
3. 검색: 전수 스캔 허용 여부(서버 부하 정책).  
4. 쓰기 작업 범위를 이후 스프린트에 포함할지 여부.  

---

*문서 버전: 0.4 — Java 17 / Spring Boot, Streamable HTTP, SVNKit 기반 저장소 접근; `application.yml` 기반 SVN·저장소 설정*

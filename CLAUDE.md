# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

포트폴리오용 Spring Boot 병원 관리 시스템. 실제 서비스 아님.
- **포트**: 8082
- **DB**: H2 인메모리 (`jdbc:h2:mem:hospitaldb`) — 재시작 시 초기화됨
- **H2 콘솔**: `http://localhost:8082/h2-console` (계정: sa / 비밀번호 없음)

## 실행 명령어

```powershell
# 애플리케이션 실행 (Windows PowerShell)
.\gradlew.bat bootRun

# 빌드
.\gradlew.bat build

# 테스트
.\gradlew.bat test
```

> **포트 충돌 시**: `netstat -ano | Select-String ":8082"` 로 PID 확인 후 `Stop-Process -Id <PID> -Force`

## 아키텍처

**레이어 구조**: Controller → Repository (Service 레이어 없음 — 의도적 단순화)

- Spring Boot 4.0.3 / Java 17 / Gradle
- Spring Data JPA + H2 + Lombok + Jakarta Validation
- Thymeleaf (서버사이드 렌더링, JS 프레임워크 없음)
- Chart.js CDN (대시보드 전용)

**컨트롤러 구조**

| 컨트롤러 | 클래스 레벨 매핑 | 역할 |
|---|---|---|
| `HomeController` | 없음 | `GET /` → `index.html` (메인 홈페이지) |
| `PatientController` | `/patients` | 환자 CRUD + 상태관리 + 대시보드 |
| `AppointmentController` | `/appointments` | 예약 CRUD + 상태변경 |
| `NotificationAdvice` | `@ControllerAdvice` | 전역 알림 카운트 주입 |

**엔티티 관계**
- `Patient` (1) ──▶ `Chart` (N) : `CascadeType.ALL` — 환자 삭제 시 진료기록 함께 삭제
- `Patient` (1) ──▶ `Appointment` (N) : CascadeType 없음
- `Patient.getAge()` : `birthDate`에서 `Period.between()`으로 실시간 계산 — DB 컬럼 없음
- `PatientStatus` enum : `WAITING / IN_PROGRESS / COMPLETED / HOSPITALIZED / DISCHARGED` (각 `getDisplayName()`으로 한글명 반환)
- `AppointmentStatus` enum : `SCHEDULED / COMPLETED / CANCELLED` (각 `getDisplayName()`으로 한글명 반환)

**엔티티 필드 요약**

| 엔티티 | 주요 필드 |
|---|---|
| `Patient` | id, name, disease, birthDate, status, isPaid, admissionDate, dischargeDate, charts |
| `Appointment` | id, patient(FK), appointmentDate, appointmentTime, doctorName, purpose, status |
| `Chart` | id, patient(FK), symptom, diagnosis, prescription, chartDate, doctorName, fee(Integer, 기본값 0) |

**URL 구조**

| URL | 역할 |
|---|---|
| `GET /` | 메인 홈페이지 (index.html — 슬라이더·빠른메뉴·서비스·공지·푸터) |
| `GET /patients` | 환자 목록 (`?keyword=` 이름검색, 페이지네이션 size=5) |
| `GET /patients/{id}` | 환자 상세 + 진료기록 |
| `POST /patients` | 환자 등록(id==null) / 수정(id!=null) |
| `GET /patients/edit/{id}` | 수정 폼 (patientList 재사용, patient 객체 주입) |
| `GET /patients/delete/{id}` | 환자 삭제 |
| `POST /patients/{id}/charts` | 진료기록 추가 |
| `GET /patients/update-status/{id}?status=` | 상태 변경 |
| `GET /patients/pay/{id}` | 수납 처리 → `isPaid=true`, `status=COMPLETED` |
| `GET /patients/hospitalize/{id}` | 입원 → `status=HOSPITALIZED`, `admissionDate=오늘` |
| `GET /patients/discharge/{id}` | 퇴원 → `status=DISCHARGED`, `dischargeDate=오늘` |
| `GET /patients/dashboard` | 대시보드 |
| `GET /appointments` | 예약 목록 (`?date=YYYY-MM-DD` 필터, size=10, appointmentDate+time 정렬) |
| `POST /appointments` | 예약 등록 (`patientId` 파라미터 별도 수신) |
| `GET /appointments/complete/{id}` | 예약 완료 처리 |
| `GET /appointments/cancel/{id}` | 예약 취소 처리 |
| `GET /appointments/delete/{id}` | 예약 삭제 |

**Repository 쿼리 메서드**

```java
// PatientRepository
Page<Patient> findByNameContaining(String name, Pageable pageable);
long countByStatus(PatientStatus status);
long countByIsPaidFalse();
long countByIsPaidFalseAndStatus(PatientStatus status);

// AppointmentRepository
Page<Appointment> findByAppointmentDate(LocalDate date, Pageable pageable);
long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

// ChartRepository
List<Chart> findByPatientId(Long patientId);
@Query("SELECT COUNT(DISTINCT c.patient.id) FROM Chart c WHERE c.chartDate = :today")
long countDistinctPatientByChartDate(@Param("today") LocalDate today); // 오늘 방문 환자 수 (중복 제거)
```

**전역 모델 주입 — NotificationAdvice**

`@ControllerAdvice`로 모든 페이지에 자동 주입:
- `notifUnpaid` = `status=COMPLETED && isPaid=false` 환자 수
- `notifTodayAppts` = 오늘 날짜 `status=SCHEDULED` 예약 수
- `notifCount` = 위 두 값의 합 (알림 벨 숫자)

**대시보드 Model 속성 — PatientController.getDashboard()**

```
totalPatients      — 전체 환자 수
todayVisitors      — 오늘 진료 기록이 있는 환자 수 (ChartRepository.countDistinctPatientByChartDate)
hospitalizedCount  — 입원 중(HOSPITALIZED) 환자 수
dischargedCount    — 퇴원(DISCHARGED) 환자 수
waitingCount       — 대기(WAITING) 환자 수
inProgressCount    — 진료 중(IN_PROGRESS) 환자 수
completedCount     — 진료 완료(COMPLETED) 환자 수
```

## 인코딩 주의사항

- `data.sql`은 **반드시 UTF-8**로 저장해야 함. `spring.sql.init.encoding=UTF-8` 설정이 없으면 Windows 기본 인코딩(EUC-KR)으로 읽어 DB 한글 데이터가 깨짐.
- `application.properties`도 UTF-8로 저장. 한글 주석 포함 시 깨질 수 있으므로 주석은 영문 권장.
- `build.gradle`의 `tasks.withType(JavaCompile) { options.encoding = 'UTF-8' }` — Java 소스 컴파일 인코딩 설정.
- 현재 `application.properties`에 `server.servlet.encoding.force=true` 설정으로 응답 인코딩 강제 UTF-8 적용 중.

## 정적 리소스

`src/main/resources/static/images/` 에 위치:

| 파일 | 용도 |
|---|---|
| `hospitalmain.jpg` | index.html 히어로 슬라이더 배경 이미지 (원본: hospitalmain.jfif → jpg 변환 복사) |
| `hospital-desk.jpg` | 예비 이미지 (현재 미사용) |

Thymeleaf에서 `/images/파일명` 경로로 참조. CSS에서는 `url('/images/파일명')`.

## 샘플 데이터

`src/main/resources/data.sql` — 앱 시작마다 자동 실행 (`spring.sql.init.mode=always`).  
환자 10명(상태별 2명씩) + 예약 10건(SCHEDULED 7·COMPLETED 2·CANCELLED 1) + 진료기록 14건. H2 인메모리이므로 재시작 시 항상 초기화됨.

## 템플릿 구조

6개 Thymeleaf 템플릿이 공통 디자인 시스템을 공유하되, **Thymeleaf fragment/layout을 사용하지 않고** 각 파일에 CSS·JS를 복사해 포함.  
→ 헤더, 알림 벨, 파티클 Canvas, 토스트 등 공통 요소를 수정할 때 **관련 파일 모두** 수정해야 함.

| 파일 | 용도 |
|---|---|
| `index.html` | 메인 홈페이지 (GNB·히어로 슬라이더·빠른메뉴·서비스·공지·푸터) |
| `patientList.html` | 환자 목록 + 등록/수정 폼 |
| `patientDetail.html` | 환자 상세 + 진료기록 |
| `dashboard.html` | 통계 대시보드 (Chart.js) |
| `appointments.html` | 예약 관리 |
| `error/404.html` | 404 에러 페이지 |

**공통 디자인 요소**
- 헤더: `linear-gradient(135deg, #0d2d5e → #1a6fc4 → #00b8a9)` + Canvas 파티클 애니메이션
- 배경: 다중 `radial-gradient` + `#e8f1fc`
- 카드/패널: Glassmorphism — `rgba(255,255,255,0.68)`, `backdrop-filter: blur(18px)`
- 버튼: 3D 눌림 — `box-shadow: 0 5px 0 rgba(0,0,0,0.16)`, `:active`에서 `translateY(4px)`
- CSS 변수: `--primary #1a6fc4`, `--accent #00b8a9`, `--danger #e74c3c`, `--warning #f39c12`, `--success #27ae60`, `--purple #7d3c98`
- 반응형 브레이크포인트: 1024px / 768px / 480px

**템플릿별 특이사항**
- `index.html` : 일반 병원 홈페이지 스타일 (Glassmorphism·파티클 없음). 히어로 슬라이더 배경은 `hospitalmain.jpg` + 슬라이드별 `::before` 색상 오버레이. 슬라이드 콘텐츠에 `position: relative; z-index: 1` 필수.
- `patientList.html` : 등록/수정 폼과 목록 테이블이 한 페이지 공존. `th:if="${patient.id == null}"` 로 모드 분기. 공지 팝업 포함.
- `patientDetail.html` : 입원 버튼 조건 `status != null` 중복 체크 (기능 정상, 미완료 이슈)
- `dashboard.html` : `th:inline="javascript"`로 서버 데이터를 Chart.js에 주입. 공지 배너(`sessionStorage bannerDismissed_dash`).
- `appointments.html` : 오늘 날짜 행 강조, 상태별 뱃지 3종

**토스트 알림 패턴**

Controller에서 `RedirectAttributes`로 전달 → 템플릿의 숨김 div에서 JS로 읽어 표시:
```java
// Controller
ra.addFlashAttribute("toast", "✔ 메시지");
ra.addFlashAttribute("toastType", "success"); // success|danger|warning|info
```
```html
<!-- 템플릿 -->
<div id="toast-data" th:if="${toast}"
     th:attr="data-msg=${toast},data-type=${toastType}" style="display:none"></div>
```

**공지 팝업 (`patientList.html`)**
- localStorage 키: `noticeSkipDate` (값: `YYYY-MM-DD`)
- 오늘 날짜와 일치하면 팝업 미표시, 다른 날짜면 표시
- `closeNoticePopup(checkSkip)` 함수: `checkSkip=true`일 때만 체크박스 상태 확인해 저장
- 현재 `patientList.html`에만 구현 — 다른 페이지에 추가 시 동일 키 사용하면 연동됨

## 업무 원칙

- 코드 생성 전 반드시 사용자 확인 후 허락하에 생성
- 항상 완성 코드 형태로 제공 (일부분 제공 금지)
- 기존 코드 기반으로 오류 검사 선행 후 수정·보완·생성
- 모든 답변은 한글로 작성

## 미완료 이슈

| 우선순위 | 항목 |
|---|---|
| 🟡 | `patientDetail.html` 입원 버튼 조건 중복 (`status != null` 체크 두 번) — 기능상 문제 없음 |
| 🔵 | Security / 로그인 기능 없음 (포트폴리오용 의도적 제외) |
| 💡 | 공지 팝업 `dashboard.html`, `appointments.html`, `patientDetail.html` 에도 추가 가능 |

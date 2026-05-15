# AI 인수인계 노트 — 스마트 병원 관리 시스템

> 이 문서는 다른 AI가 이 프로젝트를 즉시 파악하고 작업을 이어받기 위한 컨텍스트 파일입니다.
> 작성 기준: 2026-05-15

---

## 1. 프로젝트 개요

포트폴리오용 **Spring Boot 병원 관리 시스템**. 실제 서비스 아님.  
로그인/보안 기능 의도적으로 제외(포트폴리오 목적).

| 항목 | 내용 |
|---|---|
| 실행 포트 | **8082** |
| DB | H2 인메모리 (`jdbc:h2:mem:hospitaldb`) — 재시작 시 초기화 |
| H2 콘솔 | `http://localhost:8082/h2-console` (sa / 비밀번호 없음) |
| 빌드 도구 | Gradle (Windows: `gradlew.bat bootRun`) |

---

## 2. 기술 스택

- **Spring Boot 4.0.3** / **Java 25**
- Spring Data JPA + H2 + Lombok + Jakarta Validation
- Thymeleaf (서버사이드 렌더링, JS 프레임워크 없음)
- Chart.js CDN (대시보드 차트 전용)

---

## 3. 프로젝트 구조

```
src/main/java/com/example/hospital/
├── HospitalApplication.java
├── entity/
│   ├── Patient.java          ← 핵심 엔티티
│   ├── PatientStatus.java    ← enum
│   ├── Chart.java            ← 진료 기록 엔티티
│   ├── Appointment.java      ← 예약 엔티티
│   └── AppointmentStatus.java← enum
├── repository/
│   ├── PatientRepository.java
│   ├── ChartRepository.java
│   └── AppointmentRepository.java
└── controller/
    ├── PatientController.java
    ├── AppointmentController.java
    └── NotificationAdvice.java  ← @ControllerAdvice (전역 모델 주입)

src/main/resources/
├── application.properties
├── data.sql                  ← 샘플 데이터 (앱 시작 시 자동 실행)
└── templates/
    ├── patientList.html      ← 환자 목록 + 등록/수정 폼
    ├── patientDetail.html    ← 환자 상세 + 진료기록
    ├── dashboard.html        ← 통계 대시보드
    ├── appointments.html     ← 예약 관리
    └── error/404.html
```

**아키텍처**: Controller → Repository (Service 레이어 없음, 의도적 단순화)

---

## 4. 엔티티 상세

### Patient

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 자동 증가 |
| name | String | 이름 (필수) |
| disease | String | 병명 (필수) |
| birthDate | LocalDate | 생년월일 (필수, 과거 날짜) |
| status | PatientStatus | 상태 (기본값: WAITING) |
| isPaid | boolean | 수납 여부 (기본값: false) |
| admissionDate | LocalDate | 입원일 (nullable) |
| dischargeDate | LocalDate | 퇴원일 (nullable) |
| charts | List\<Chart\> | 진료 기록 목록 (CascadeType.ALL) |

- `getAge()` : `birthDate`에서 `Period.between()`으로 나이 계산 — **DB 컬럼 없음**, 매번 계산

### PatientStatus (enum)

| 값 | displayName |
|---|---|
| WAITING | 대기 중 |
| IN_PROGRESS | 진료 중 |
| COMPLETED | 진료 완료 |
| HOSPITALIZED | 입원 중 |
| DISCHARGED | 퇴원 |

### Chart (진료 기록)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | |
| symptom | String | 증상 |
| diagnosis | String | 진단명 |
| prescription | String | 처방 |
| chartDate | LocalDate | 진료일 |
| doctorName | String | 담당 의사 |
| fee | Integer | 진료비 (기본값 0) |
| patient | Patient (FK) | @ManyToOne |

### Appointment (예약)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | |
| patient | Patient (FK) | @ManyToOne |
| appointmentDate | LocalDate | 예약 날짜 |
| appointmentTime | LocalTime | 예약 시간 |
| doctorName | String | 담당 의사 |
| purpose | String | 내원 목적 |
| status | AppointmentStatus | SCHEDULED / COMPLETED / CANCELLED |

---

## 5. Repository 메서드 목록

### PatientRepository
```java
Page<Patient> findByNameContaining(String name, Pageable pageable)
long countByStatus(PatientStatus status)
long countByIsPaidFalse()
long countByIsPaidFalseAndStatus(PatientStatus status)  // 알림 벨 전용
```

### ChartRepository
```java
List<Chart> findByPatientId(Long patientId)
long countDistinctPatientByChartDate(LocalDate date)  // 오늘 방문자 수
```

### AppointmentRepository
```java
Page<Appointment> findByAppointmentDate(LocalDate date, Pageable pageable)
long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status)
```

---

## 6. URL 구조 (전체)

### 환자 관리 (`PatientController` — `/patients`)

| Method | URL | 동작 |
|---|---|---|
| GET | `/patients` | 환자 목록 (이름 검색 `?keyword=`, 페이지네이션 `size=5`) |
| POST | `/patients` | 환자 등록 (id==null) / 수정 (id!=null) |
| GET | `/patients/{id}` | 환자 상세 + 진료기록 |
| GET | `/patients/edit/{id}` | 수정 폼 (`patientList` 재사용, patient 객체 주입) |
| GET | `/patients/delete/{id}` | 환자 삭제 (Chart CascadeType.ALL로 함께 삭제) |
| POST | `/patients/{id}/charts` | 진료 기록 추가 |
| GET | `/patients/update-status/{id}?status=` | 상태 변경 |
| GET | `/patients/pay/{id}` | 수납 처리 → `isPaid=true`, `status=COMPLETED` |
| GET | `/patients/hospitalize/{id}` | 입원 → `status=HOSPITALIZED`, `admissionDate=오늘` |
| GET | `/patients/discharge/{id}` | 퇴원 → `status=DISCHARGED`, `dischargeDate=오늘` |
| GET | `/patients/dashboard` | 대시보드 통계 페이지 |

### 예약 관리 (`AppointmentController` — `/appointments`)

| Method | URL | 동작 |
|---|---|---|
| GET | `/appointments` | 예약 목록 (날짜 필터 `?date=YYYY-MM-DD`, 페이지네이션 `size=10`) |
| POST | `/appointments` | 예약 등록 (`patientId` 파라미터 별도 받음) |
| GET | `/appointments/complete/{id}` | 예약 완료 처리 → `status=COMPLETED` |
| GET | `/appointments/cancel/{id}` | 예약 취소 → `status=CANCELLED` |
| GET | `/appointments/delete/{id}` | 예약 삭제 |

---

## 7. 전역 모델 주입 — NotificationAdvice

`@ControllerAdvice`로 **모든 페이지에 자동 주입**되는 변수:

| 변수명 | 내용 |
|---|---|
| `notifCount` | `notifUnpaid + notifTodayAppts` (알림 벨 숫자) |
| `notifUnpaid` | `status=COMPLETED && isPaid=false` 환자 수 |
| `notifTodayAppts` | 오늘 날짜 `status=SCHEDULED` 예약 수 |

템플릿에서 `th:if="${notifCount > 0}"` 조건으로 알림 벨 활성화.

---

## 8. 템플릿 공통 디자인 시스템

4개 Thymeleaf 템플릿이 완전히 같은 디자인 언어를 공유:

### CSS 변수 (`:root`)
```css
--primary: #1a6fc4
--primary-dark: #155a9e
--primary-light: #e8f1fb
--accent: #00b8a9
--danger: #e74c3c
--warning: #f39c12
--success: #27ae60
--purple: #7d3c98
```

### 공통 요소
- **헤더**: `linear-gradient(135deg, #0d2d5e → #1a6fc4 → #00b8a9)` + Canvas 파티클 애니메이션
- **배경**: 다중 `radial-gradient` + `#e8f1fc` 기본색
- **카드/패널**: Glassmorphism — `rgba(255,255,255,0.68)`, `backdrop-filter: blur(18px)`, `border: 1px solid rgba(255,255,255,0.55)`
- **버튼**: 3D 눌림 효과 — `box-shadow: 0 5px 0 rgba(0,0,0,0.16)` + `:active`에서 `translateY(4px)`
- **토스트 알림**: `RedirectAttributes`로 `toast` + `toastType` 전달, 페이지마다 `<div id="toast-data">` 읽어서 표시
- **알림 벨**: 헤더 우측, dropdown 형태, `sessionStorage` / `localStorage` 활용

### 각 템플릿 특이사항

**patientList.html**
- 등록 폼과 목록 테이블이 한 페이지에 공존
- `th:if="${patient.id == null}"` 로 "환자 등록" / "환자 수정" 모드 분기
- 삭제 확인 모달: 커스텀 CSS + JS (브라우저 기본 confirm 아님)
- **공지 팝업** 있음 (localStorage `noticeSkipDate` 키, 오늘 하루 보지 않기 기능)

**patientDetail.html**
- 환자 기본정보 카드 + 진료기록 추가 폼 + 진료기록 목록
- 입원 버튼 조건 중복 (`status != null` 체크 2번) — 기능 정상, 미완료 이슈

**dashboard.html**
- Chart.js 막대차트(statusChart) + 도넛차트(donutChart)
- `th:inline="javascript"`로 서버 데이터를 JS 변수에 주입
- 숫자 카운트업 애니메이션 (countUp 함수)
- 공지 배너(`infoBanner`): `sessionStorage bannerDismissed_dash`로 세션 내 닫기

**appointments.html**
- 오늘 날짜 행 강조 (`th:class="${appointment.appointmentDate == today}"`)
- 상태별 뱃지: SCHEDULED(파랑) / COMPLETED(초록) / CANCELLED(빨강)

---

## 9. 샘플 데이터 (`data.sql`)

앱 시작 시 자동 실행 (`spring.sql.init.mode=always`).  
H2 인메모리 → 재시작마다 아래 데이터로 초기화됨.

- 환자 10명 (상태별 골고루: WAITING×2, IN_PROGRESS×2, HOSPITALIZED×2, COMPLETED×2, DISCHARGED×2)
- 예약 10건 (오늘 3건, 내일 2건, 모레 1건, 과거 완료/취소 포함)
- 진료 기록 13건 (각 환자별 1~2건)

---

## 10. 현재 완료된 기능

- [x] 환자 CRUD (등록, 조회, 수정, 삭제)
- [x] 이름 검색 + 페이지네이션
- [x] 진료 기록 추가 (환자 상세 페이지)
- [x] 상태 변경 (WAITING→IN_PROGRESS→COMPLETED 등)
- [x] 수납 처리 (isPaid=true, status=COMPLETED)
- [x] 입원/퇴원 처리 (admissionDate, dischargeDate 자동 기록)
- [x] 대시보드 (통계 카드 4개, 상태 요약 바, 차트 2종)
- [x] 예약 관리 (등록, 완료, 취소, 삭제, 날짜 필터)
- [x] 전역 알림 벨 (미수납 + 오늘 예약 카운트)
- [x] 토스트 알림 (모든 액션 후 표시)
- [x] 공지 팝업 (patientList.html — localStorage 오늘 하루 보지 않기)

---

## 11. 미완료 이슈 및 개선 가능 항목

| 우선순위 | 항목 |
|---|---|
| 🟡 낮음 | `patientDetail.html` 입원 버튼 조건 `status != null` 체크 2번 중복 — 기능 정상 |
| 🔵 의도적 제외 | Security / 로그인 기능 없음 (포트폴리오용) |
| 💡 추가 가능 | 공지 팝업을 `dashboard.html`, `appointments.html`, `patientDetail.html`에도 추가 |
| 💡 추가 가능 | 교대 근무 인수인계 페이지 (입원 환자 목록 + 미수납 현황 인쇄용) |
| 💡 추가 가능 | 진료비 합계 통계 (Chart.java의 fee 필드 활용) |
| 💡 추가 가능 | 예약 알림 이메일 (현재 UI 알림만 있음) |

---

## 12. 실행 방법

```bash
# Windows
gradlew.bat bootRun

# 접속
http://localhost:8082/patients      ← 메인 (환자 목록)
http://localhost:8082/patients/dashboard
http://localhost:8082/appointments
http://localhost:8082/h2-console    ← DB 콘솔 (JDBC URL: jdbc:h2:mem:hospitaldb)
```

---

## 13. 작업 시 주의사항

1. **H2 인메모리 DB** — 서버 재시작 시 데이터 초기화됨. 재현이 필요한 데이터는 `data.sql` 수정.
2. **Service 레이어 없음** — Controller에서 Repository 직접 호출. 새 기능 추가 시 이 패턴 유지.
3. **Thymeleaf 템플릿** — 공통 디자인 요소(헤더, 알림 벨, 토스트, 파티클 canvas)가 4개 파일에 **복사**되어 있음. 공통 요소 수정 시 4개 파일 모두 수정 필요.
4. **토스트 알림 패턴** — Controller에서 `ra.addFlashAttribute("toast", "메시지")` + `ra.addFlashAttribute("toastType", "success|danger|warning|info")` → 템플릿의 `<div id="toast-data">` 에서 JS로 읽어 표시.
5. **공지 팝업** — `patientList.html`에만 구현됨. localStorage 키: `noticeSkipDate` (값: `YYYY-MM-DD`).
6. **페이지네이션** — 환자 목록 `size=5`, 예약 목록 `size=10`. URL에 `?page=0&size=5` 형태로 전달.
7. **빌드 오류 시** — Java 25 + Spring Boot 4.x 조합. `gradlew.bat clean build` 후 재시도.

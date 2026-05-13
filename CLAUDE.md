# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

포트폴리오용 Spring Boot 병원 관리 시스템. 실제 서비스 아님.
- **포트**: 8082
- **DB**: H2 인메모리 (`jdbc:h2:mem:hospitaldb`) — 재시작 시 초기화됨
- **H2 콘솔**: `http://localhost:8082/h2-console`

## 실행 명령어

```bash
# 애플리케이션 실행 (Windows)
gradlew.bat bootRun

# 빌드
gradlew.bat build

# 테스트
gradlew.bat test
```

## 기술 스택

- Spring Boot 4.0.3 / Java 25 / Gradle
- Spring Data JPA + H2 + Lombok + Validation
- Thymeleaf (서버사이드 렌더링, JS 프레임워크 없음)
- Chart.js CDN (대시보드 차트)

## 아키텍처

**레이어 구조**: Controller → Repository (Service 레이어 없음)

```
PatientController  ──▶  PatientRepository
                   ──▶  ChartRepository
```

**엔티티 관계**
- `Patient` (1) ──▶ `Chart` (N) : `CascadeType.ALL` (환자 삭제 시 진료기록 함께 삭제)
- `Patient.getAge()` : `birthDate` 필드에서 `Period.between()`으로 자동 계산하는 transient 메서드 — DB에 저장되지 않음
- `PatientStatus` enum : `WAITING / IN_PROGRESS / COMPLETED / HOSPITALIZED / DISCHARGED`

**URL 구조**

| URL | 역할 |
|---|---|
| `GET /patients` | 환자 목록 (페이지네이션, 이름 검색) |
| `GET /patients/{id}` | 환자 상세 + 진료기록 |
| `POST /patients` | 환자 등록 / 수정 |
| `GET /patients/edit/{id}` | 수정 폼 (patientList 재사용) |
| `GET /patients/delete/{id}` | 환자 삭제 |
| `POST /patients/{id}/charts` | 진료기록 추가 |
| `GET /patients/update-status/{id}?status=` | 상태 변경 |
| `GET /patients/pay/{id}` | 수납 처리 → status=COMPLETED, isPaid=true |
| `GET /patients/hospitalize/{id}` | 입원 처리 → admissionDate=오늘 |
| `GET /patients/discharge/{id}` | 퇴원 처리 → dischargeDate=오늘 |
| `GET /patients/dashboard` | 대시보드 |
| `GET /appointments` | 예약 목록 (date 필터, 페이지네이션) |
| `POST /appointments` | 예약 등록 |
| `GET /appointments/complete/{id}` | 예약 완료 처리 |
| `GET /appointments/cancel/{id}` | 예약 취소 처리 |
| `GET /appointments/delete/{id}` | 예약 삭제 |

**알림 벨 (전역)**
- `NotificationAdvice` (@ControllerAdvice) 가 모든 페이지에 `notifCount`, `notifUnpaid`, `notifTodayAppts` 자동 주입
- `notifUnpaid` = 진료 완료(COMPLETED) 상태이면서 수납 미완료인 환자 수
- `notifTodayAppts` = 오늘 날짜 SCHEDULED 예약 수

## 샘플 데이터

`src/main/resources/data.sql` — 앱 시작마다 자동 실행됨 (`spring.sql.init.mode=always`).
H2 인메모리이므로 재시작 시 항상 초기 10명 환자 + 진료기록이 새로 삽입됨.

## 템플릿 구조

4개 Thymeleaf 템플릿이 공통 디자인 시스템을 공유함:
- **공통 요소**: Canvas 기반 파티클 헤더, CSS 변수(`--primary`, `--accent` 등), Glassmorphism 패널, 3D 버튼
- `patientList.html` : 등록/수정 폼 + 목록 테이블이 한 페이지에 공존. `patient.id == null` 여부로 등록/수정 모드 분기
- `patientDetail.html` : 환자 기본정보 + 진료기록 추가 폼 + 진료기록 목록
- `dashboard.html` : Chart.js 막대차트 + 통계카드. `th:inline="javascript"`로 서버 데이터 주입
- `appointments.html` : 예약 등록 폼 + 예약 목록. 오늘 행 강조, 상태별 뱃지(SCHEDULED/COMPLETED/CANCELLED)

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
| ✅ | 예약(Appointment) 기능 추가 완료 — AppointmentController + appointments.html |

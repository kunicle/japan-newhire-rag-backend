# 조직도 및 HR 조직 편집 구현 보고

1. **현재 구조 분석 결과**

   두 저장소의 로컬 develop에서 시작했으며 미커밋 변경은 없었다.
   Department는 상위 부서·고유 코드·이름·상태·표시 순서·삭제 시점을 지원한다.
   Employee는 부서·직급 FK와 이름·입사일을 갖는다. JobGrade는 grade_level과 활성 여부를 갖는다.
   ManagerRelation은 DIRECT/TEAM 및 ACTIVE/ENDED 이력을 관리하고 end()를 지원한다.

   OrganizationController → OrganizationTreeQueryService는 부서 트리 안에 직원 목록을 반환했다.
   타 도메인용 OrganizationQueryService/EmployeeSummary 공개 계약은 수정하지 않았다.
   기존 DepartmentRepository·EmployeeRepository 조회 및 JobGradeRepository 활성 직급 조회를 재사용했다.
   HrEmployeeManagerController/DirectManagerCommandService는 상급자 변경·이력·감사를 지원했으나 해제·순환·직급 검증은 없었다.
   CurrentUserProvider/CurrentUserContext의 appUserId·roles와 AuditLogRecordService 공개 계약을 재사용했다.
   SecurityConfig에 역할 자동 승격을 추가하지 않았다.

   프론트 OrganizationPage는 부서 선택 및 직원 목록이었다.
   organizationApi의 공통 request(), useCurrentRoles(), Button/Skeleton 및 CSS 모듈을 재사용했다.
   공통 Card는 존재하지만 조직도에는 고정 크기 전용 카드 CSS를 사용했다.
   공통 모달은 없어 네이티브 dialog 기반 편집 모달을 추가했다.
   직급 방향은 seed로 확인할 수 없어 사용자 확인을 받았으며, 작은 grade_level이 높은 직급이다.

2. **변경한 Backend 파일**

- [SecurityConfig.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/auth/config/SecurityConfig.java)
- [OrganizationEmployeeResponse.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/controller/dto/OrganizationEmployeeResponse.java)
- [Department.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/entity/Department.java)
- [Employee.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/entity/Employee.java)
- [OrganizationErrorCode.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/error/OrganizationErrorCode.java)
- [DepartmentRepository.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/repository/DepartmentRepository.java)
- [EmployeeRepository.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/repository/EmployeeRepository.java)
- [ManagerRelationRepository.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/repository/ManagerRelationRepository.java)
- [DirectManagerCommandService.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/DirectManagerCommandService.java)
- [OrganizationTreeQueryService.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/OrganizationTreeQueryService.java)
- [AuditActionType.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/system/audit/enums/AuditActionType.java)
- [AuditTargetType.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/system/audit/enums/AuditTargetType.java)
- [HrEmployeeManagerControllerTest.java](../src/test/java/com/teamproject/japan_newhire_rag_backend/domain/organization/controller/HrEmployeeManagerControllerTest.java)
- [DirectManagerCommandServiceTest.java](../src/test/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/DirectManagerCommandServiceTest.java)
- [OrganizationTreeQueryServiceTest.java](../src/test/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/OrganizationTreeQueryServiceTest.java)
- [HrOrganizationController.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/controller/HrOrganizationController.java)
- [ChangeEmployeeOrganizationRequest.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/controller/dto/ChangeEmployeeOrganizationRequest.java)
- [CreateDepartmentRequest.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/controller/dto/CreateDepartmentRequest.java)
- [UpdateDepartmentRequest.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/controller/dto/UpdateDepartmentRequest.java)
- [DepartmentCommandService.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/DepartmentCommandService.java)
- [EmployeeOrganizationCommandService.java](../src/main/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/EmployeeOrganizationCommandService.java)
- [DepartmentCommandServiceTest.java](../src/test/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/DepartmentCommandServiceTest.java)
- [EmployeeOrganizationCommandServiceTest.java](../src/test/java/com/teamproject/japan_newhire_rag_backend/domain/organization/service/internal/EmployeeOrganizationCommandServiceTest.java)

3. **변경한 Frontend 파일**

- [AuditPage.tsx](../../japan-newhire-rag-frontend/src/features/audit/AuditPage.tsx)
- [auditViewHelpers.ts](../../japan-newhire-rag-frontend/src/features/audit/auditViewHelpers.ts)
- [types.ts](../../japan-newhire-rag-frontend/src/features/audit/types.ts)
- [OrganizationPage.module.css](../../japan-newhire-rag-frontend/src/features/organization/OrganizationPage.module.css)
- [OrganizationPage.tsx](../../japan-newhire-rag-frontend/src/features/organization/OrganizationPage.tsx)
- [organizationApi.test.ts](../../japan-newhire-rag-frontend/src/features/organization/organizationApi.test.ts)
- [organizationApi.ts](../../japan-newhire-rag-frontend/src/features/organization/organizationApi.ts)
- [organizationViewHelpers.ts](../../japan-newhire-rag-frontend/src/features/organization/organizationViewHelpers.ts)
- [types.ts](../../japan-newhire-rag-frontend/src/features/organization/types.ts)
- [OrganizationEditDialog.tsx](../../japan-newhire-rag-frontend/src/features/organization/OrganizationEditDialog.tsx)
- [OrganizationPage.test.tsx](../../japan-newhire-rag-frontend/src/features/organization/OrganizationPage.test.tsx)
- [organizationChart.test.ts](../../japan-newhire-rag-frontend/src/features/organization/organizationChart.test.ts)
- [organizationChart.ts](../../japan-newhire-rag-frontend/src/features/organization/organizationChart.ts)

4. **새로 추가한 API**

   | API | 성공 상태 | 용도 |
   | --- | --- | --- |
   | PATCH /api/hr/employees/{employeeId}/organization | 204 | 부서·직급·직속 상급자 변경 |
   | POST /api/hr/departments | 201 | 부서 생성 |
   | PATCH /api/hr/departments/{departmentId} | 200 | 부서명·상위 부서 변경 |

   직원 변경: departmentId·jobGradeId 필수, managerEmployeeId=null은 해제.
   부서 생성: departmentCode·departmentName 필수, parentDepartmentId 선택.
   부서 수정: departmentName 필수, parentDepartmentId=null은 최상위 부서.
   기존 조직 조회·직급 조회·상급자 변경 API는 유지한다.
   기존 manager API의 필수 managerEmployeeId 계약은 유지하고 새 organization API에서 해제를 지원한다.
   조직 응답은 employeeName 등 기존 필드를 유지하고 departmentName·managerEmployeeId만 추가했다.

5. **권한 정책**

   조회는 기존 로그인 사용자 정책을 유지한다.
   편집은 SecurityConfig와 Controller의 hasRole('HR_MANAGER')로 보호한다.
   EMPLOYEE·MANAGER·SYSTEM_ADMIN 단독 역할은 403, 미인증은 401이다.
   SYSTEM_ADMIN에 HR 권한을 자동 부여하지 않는다.
   프론트는 roles.includes('HR_MANAGER')로 편집 버튼·모달·부서 관리 UI를 제어한다.

6. **조직도 트리 생성 방식**

   활성 DIRECT 관계로 부모-자식을 연결하고 부서 간 상급자 관계도 유지한다.
   부서별 분리 대신 전체 트리와 부서 필터를 사용하며 드롭다운 들여쓰기로 기존 부서 계층을 표현한다.
   직급 ASC → 이름 → employeeId로 안정적으로 정렬한다.
   상급자 없는 직원은 직급에 맞는 높이에 배치하며 임의의 상급자를 만들지 않는다.
   실제 DIRECT 관계가 직급과 모순되는 기존 데이터는 DIRECT 관계를 우선한다.
   필터/검색 결과에는 실제 상급자를 함께 표시하며 중복 카드를 방지한다.
   서브트리 너비로 형제 노드 겹침을 방지하고 SVG 연결선을 표시한다.
   고정 카드 너비와 가로·세로 스크롤을 지원한다.
   카드에는 부서·직급·이름·입사일만 표시한다.
   기존 순환 데이터는 문제 연결선을 끊고 경고하여 무한 재귀를 방지한다.

7. **HR_MANAGER 편집 방식**

   조직 편집 → 직원 카드 편집 → 부서·직급·상급자 선택 → 저장.
   활성 직급 조회 API를 사용하며 새 직급 CRUD는 추가하지 않았다.
   이름·입사일은 수정하지 않는다.
   부서 생성 및 부서 선택 후 부서 수정으로 부서명·상위 부서를 관리한다.
   생성 시 기존 NOT NULL/UNIQUE 스키마에 필요한 부서 코드를 받는다.
   저장 중 버튼을 비활성화하고 실패 시 입력을 유지하며 성공 시 재조회한다.
   네이티브 dialog로 포커스, Escape/취소 및 포커스 복귀를 처리한다.

8. **Validation 처리**

   누락·비양수 ID 및 문자열 제약 위반: 400.
   없는/삭제된 직원·부서·상급자, 없는/비활성 직급: 404.
   자기 지정은 기존 SELF_MANAGER_NOT_ALLOWED의 409를 유지한다.
   상급자 순환·부서 순환·중복 부서 코드: 409.
   상급자 grade_level이 직원보다 크면 낮은 직급이므로 409로 거부한다. 동일 직급은 허용한다.
   직급 변경은 새 직급으로 상급자를 검증하며 직속 부하보다 낮아지는 변경도 거부한다.
   프론트도 자신·하위 보고자·낮은 직급 상급자를 후보에서 제외한다.
   직원 조직 변경과 상급자 변경은 한 트랜잭션으로 함께 롤백된다.
   동시 편집 순환 방지를 위해 직원/부서 행을 ID 순으로 잠근다.

9. **Audit 처리**

   AuditLogRecordService/AuditLogRecordCommand 및 DIRECT_MANAGER_CHANGED를 재사용한다.
   EMPLOYEE_DEPARTMENT_CHANGED·EMPLOYEE_JOB_GRADE_CHANGED·DEPARTMENT_CREATED·DEPARTMENT_UPDATED와 DEPARTMENT 대상을 추가했다.
   허용된 키의 변경 전후 값만 기록한다.
   상급자 해제는 기존 관계를 ENDED로 종료하고 managerEmployeeId=null을 감사 기록한다.
   동일 값에는 중복 감사를 남기지 않는다.
   감사 화면 타입·필터·한글 라벨도 확장했다.

10. **DB 변경 여부**

    스키마 변경, ALTER SQL, 기존 DDL 덮어쓰기 없음.
    RDS 연결·데이터 수정·삭제·DDL 실행 없음.
    감사 action_type/target_type은 기존 VARCHAR(50) 범위에서 확장했다.

11. **Backend 테스트 결과**

    DB 비의존 전체 회귀 테스트: 1,288개 통과, 실패 0.
    최종 import 정리 후 조직·권한·감사 관련 테스트: 101개 통과, 실패 0.
    기존 DirectManager와 신규 직원 변경·해제·순환·직급·부서·접근 제어 테스트를 포함한다.

    실행 명령(Windows):
    ./mvnw.cmd '-Dtest=*,!JapanNewhireRagBackendApplicationTests,!AuthIntegrationTest,!ManagerEducationIntegrationTest,!EvaluationRepositoryIntegrationTest,!EvaluationPublishTransactionIntegrationTest,!LearningProgressTransactionTest,!CourseEnrollmentTransactionTest,!CourseRepositoryTest' test

    전체 mvnw test는 실행하지 않았다. 위 8개 클래스는 실제 DB에 연결하는 SpringBootTest이며 일부는 INSERT/DELETE를 수행한다.
    현재 RDS 데이터를 변경하지 말라는 요청에 따라 제외했다.

12. **Frontend 테스트/build 결과**

    npm test: 55개 파일, 503개 테스트 통과.
    npm run build: TypeScript 검사 및 Vite 빌드 성공.
    npm run lint: 오류 없음.
    500 kB 초과 번들 경고는 남아 있다(최종 JS 약 541 kB, gzip 약 148 kB).
    테스트는 직급 순서, 부서 간 연결, 100명 형제 노드 겹침 방지, 순환 방어,
    검색 시 상급자 유지, 역할별 UI, 해제 저장·재조회, 저장 실패를 검증한다.

13. **기존 기능 영향 여부**

    로그인·JWT·refresh·/api/me 구현과 공개 조직 서비스 계약을 유지한다.
    기존 조직 응답 필드와 상급자 API를 유지한다.
    B/C/D Entity·Repository 직접 참조를 추가하지 않았다.
    기존 인증·알림·교육·평가·RAG의 DB 비의존 회귀 테스트가 통과했다.
    상급자 API에는 새 정책에 따른 순환 및 낮은 직급 상급자 지정 거부가 추가되었다.

14. **아직 남은 문제**

    격리된 MySQL 테스트 DB가 없어 실제 DB 트랜잭션·동시성 통합 검증은 미수행이다.
    실제 브라우저 픽셀 배치·모바일 조작은 별도로 확인하지 않았다(DOM 동작 및 레이아웃 계산 테스트 통과).
    직원/부서 전체 행 잠금은 동시 편집 안전성을 위한 방식으로 대규모 조직에서 편집 처리 성능 검토가 필요하다.
    전체 번들 경고는 이번 범위에서 별도 코드 분할하지 않았다.

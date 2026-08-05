# 일본 신입사원 규정 안내 RAG - Backend

일본 신입사원을 위한 사내 규정 안내 RAG 서비스의 Spring Boot 백엔드입니다.

## 기술 구성

- Java 17
- Spring Boot
- Maven
- Spring Web
- Validation
- Lombok

## 요구 환경

```bash
java -version
```

Java 17이 표시되어야 합니다.

## 실행 방법

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

기본 주소:

```text
http://localhost:8080
```

## 상태 확인

서버 실행 후 다음 명령으로 확인합니다.

```bash
curl http://localhost:8080/health
```

정상 응답:

```json
{
  "service": "japan-newhire-rag-backend",
  "status": "ok"
}
```

## 테스트

```bash
./mvnw test
```

## 주의사항

다음 정보와 파일은 GitHub에 올리지 않습니다.

- `.env`
- `application-local.yml`
- `application-secret.yml`
- `application-prod.yml`
- API 키
- DB 비밀번호

import { useState } from "react";
import { login } from "../api/authApi";
import { ApiError, saveAccessToken } from "../api/client";

export default function LoginPanel() {
  const [email, setEmail] = useState(
    "bootstrap-admin@test.local",
  );
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleLogin(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();
    setLoading(true);
    setMessage("");

    try {
      const response = await login({
        email,
        password,
        deviceInfo: "frontend-mock",
      });

      saveAccessToken(response.accessToken);
      setPassword("");
      setMessage(
        "로그인 성공: Access Token을 저장했습니다.",
      );
    } catch (caughtError) {
      if (caughtError instanceof ApiError) {
        setMessage(
          `HTTP ${caughtError.status} / ${caughtError.code}: ${caughtError.message}`,
        );
      } else if (caughtError instanceof Error) {
        setMessage(caughtError.message);
      } else {
        setMessage("알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="panel">
      <h2>로그인</h2>

      <p className="endpoint">POST /api/auth/login</p>

      <form onSubmit={handleLogin}>
        <label htmlFor="login-email">이메일</label>
        <input
          id="login-email"
          type="email"
          value={email}
          onChange={(event) =>
            setEmail(event.target.value)
          }
        />

        <label htmlFor="login-password">비밀번호</label>
        <input
          id="login-password"
          type="password"
          value={password}
          onChange={(event) =>
            setPassword(event.target.value)
          }
        />

        <button type="submit" disabled={loading}>
          {loading ? "로그인 중..." : "로그인"}
        </button>
      </form>

      {message && <p className="message">{message}</p>}
    </section>
  );
}
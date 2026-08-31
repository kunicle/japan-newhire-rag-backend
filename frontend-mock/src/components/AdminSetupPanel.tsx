import { useState } from "react";
import {
  getMyProfile,
  updateUserRoles,
  type RoleType,
} from "../api/adminSetupApi";
import { ApiError } from "../api/client";

export default function AdminSetupPanel() {
  const [message, setMessage] = useState("");
  const [result, setResult] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function handleGrantHrRole() {
    setLoading(true);
    setMessage("");
    setResult(null);

    try {
      const profile = await getMyProfile();

      const roles = new Set<RoleType>(profile.roles);
      roles.add("SYSTEM_ADMIN");
      roles.add("HR_MANAGER");

      const response = await updateUserRoles(
        profile.appUserId,
        Array.from(roles),
      );

      setResult(response);
      setMessage(
        "HR_MANAGER 권한을 추가했습니다. 변경된 권한으로 JWT를 다시 발급받기 위해 위 로그인 버튼을 다시 눌러주세요.",
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
      <h2>로컬 테스트 권한 설정</h2>

      <p>
        현재 SYSTEM_ADMIN 계정에 HR_MANAGER 권한을
        추가합니다.
      </p>

      <button
        type="button"
        onClick={handleGrantHrRole}
        disabled={loading}
      >
        {loading
          ? "권한 설정 중..."
          : "현재 계정에 HR_MANAGER 추가"}
      </button>

      {message && <p className="message">{message}</p>}

      {result !== null && (
        <pre className="result-box">
          {JSON.stringify(result, null, 2)}
        </pre>
      )}
    </section>
  );
}
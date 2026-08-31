import { useState } from "react";
import { ApiError } from "../api/client";
import { getCourses } from "../api/courseApi";

export default function CourseListPanel() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [result, setResult] = useState<unknown>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleLoadCourses() {
    setLoading(true);
    setError("");
    setResult(null);

    try {
      const response = await getCourses(page, size);
      setResult(response);
    } catch (caughtError) {
      if (caughtError instanceof ApiError) {
        setError(
          `HTTP ${caughtError.status} / ${caughtError.code}\n${caughtError.message}`,
        );
      } else if (caughtError instanceof Error) {
        setError(caughtError.message);
      } else {
        setError("알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="panel">
      <h2>교육 과정 목록 조회</h2>

      <p className="endpoint">
        GET /api/hr/courses?page={page}&size={size}
      </p>

      <div className="input-row">
        <label>
          페이지
          <input
            type="number"
            min="0"
            value={page}
            onChange={(event) =>
              setPage(Number(event.target.value))
            }
          />
        </label>

        <label>
          페이지 크기
          <input
            type="number"
            min="1"
            value={size}
            onChange={(event) =>
              setSize(Number(event.target.value))
            }
          />
        </label>
      </div>

      <button
        type="button"
        onClick={handleLoadCourses}
        disabled={loading}
      >
        {loading ? "조회 중..." : "과정 목록 조회"}
      </button>

      {error && (
        <pre className="result-box error-box">{error}</pre>
      )}

      {result !== null && (
        <pre className="result-box">
          {JSON.stringify(result, null, 2)}
        </pre>
      )}
    </section>
  );
}
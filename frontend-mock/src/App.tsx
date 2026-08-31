import "./App.css";
import AdminSetupPanel from "./components/AdminSetupPanel";
import CourseListPanel from "./components/CourseListPanel";
import LoginPanel from "./components/LoginPanel";

function App() {
  return (
    <main className="app-container">
      <header className="app-header">
        <div>
          <h1>C 기능 테스트 콘솔</h1>
          <p>
            교육·온보딩 API를 검증하기 위한 임시
            프론트엔드입니다.
          </p>
        </div>

        <span className="environment-badge">
          API:{" "}
          {import.meta.env.VITE_API_BASE_URL ||
            "Vite proxy → localhost:8080"}
        </span>
      </header>

      <LoginPanel />
      <AdminSetupPanel />
      <CourseListPanel />
    </main>
  );
}

export default App;
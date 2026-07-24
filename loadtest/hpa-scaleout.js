// HPA 스케일아웃 검증용 k6 부하 스크립트 (읽기 전용, 데이터 미변경)
//
// 목적: 트래픽을 단계적으로 올려 backend CPU를 65% 임계값 위로 밀어,
//       HPA가 파드를 2 -> 6으로 늘리는지 / Cluster Autoscaler가 노드를
//       따라 확장하는지를 관측한다. 대시보드 집계 쿼리가 무거워 CPU 유발에 적합.
//
// 실행:
//   export BASE_URL=https://smtm.mang.pe.kr
//   export SMTM_USER=<시연용 계정>
//   export SMTM_PASS=<비밀번호>
//   k6 run loadtest/hpa-scaleout.js
//
// 관측(별도 터미널):
//   watch -n2 'kubectl -n smtm get hpa,pod -o wide'
//
// 단계(총 약 14분): 스케일아웃 유도 -> 유지 -> 스케일인 관측
//   - ramp/hold 구간을 길게 잡은 이유: HPA는 기본적으로 안정화 윈도우가 있어
//     (scaleUp ~여러 초, scaleDown 5분) 짧게 치면 반응 전에 끝나버린다.

import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "https://smtm.mang.pe.kr";
const USER = __ENV.SMTM_USER;
const PASS = __ENV.SMTM_PASS;
const YEAR_MONTH = __ENV.YEAR_MONTH || "202607"; // dashboard API 필수 파라미터 (yyyyMM)

const loginTrend = new Trend("login_duration", true);

export const options = {
  scenarios: {
    scaleout: {
      executor: "ramping-vus",
      startVUs: 5,
      stages: [
        { duration: "2m", target: 30 },   // 완만히 상승 — CPU 65% 돌파 유도
        { duration: "3m", target: 80 },   // 추가 상승 — max 6 파드까지 밀기
        { duration: "4m", target: 80 },   // 유지 — 스케일아웃 완료 + 안정화 관측
        { duration: "3m", target: 10 },   // 하강 — 스케일인 관측
        { duration: "2m", target: 0 },
      ],
      gracefulRampDown: "30s",
    },
  },
  thresholds: {
    // 합격 기준: 스케일아웃이 제때 되면 p95가 이 밑으로 유지되어야 한다.
    http_req_failed: ["rate<0.02"],       // 에러율 2% 미만
    http_req_duration: ["p(95)<800"],     // p95 800ms 미만
  },
};

// 각 VU가 시작될 때 한 번 로그인해 토큰을 확보한다. (setup 대신 per-VU:
// VU가 각자 세션을 갖는 실사용 패턴에 더 가깝고, 토큰 만료도 자연 회피)
export function setup() {
  if (!USER || !PASS) {
    throw new Error("SMTM_USER / SMTM_PASS 환경변수가 필요합니다.");
  }
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: USER, password: PASS }),
    { headers: { "Content-Type": "application/json" } }
  );
  loginTrend.add(res.timings.duration);
  check(res, { "login 200": (r) => r.status === 200 });
  const token = res.json("data.accessToken");
  if (!token) throw new Error(`로그인 실패: ${res.status} ${res.body}`);
  return { token };
}

export default function (data) {
  const params = {
    headers: {
      Authorization: `Bearer ${data.token}`,
      "Content-Type": "application/json",
    },
  };

  // 실사용 화면 진입 패턴을 모사: 대시보드 요약 -> 일별 추이 -> 거래목록 -> 예산
  group("dashboard", () => {
    const r1 = http.get(`${BASE_URL}/api/dashboard/summary?yearMonth=${YEAR_MONTH}`, params);
    check(r1, { "summary 200": (r) => r.status === 200 });

    const r2 = http.get(`${BASE_URL}/api/dashboard/daily?yearMonth=${YEAR_MONTH}`, params);
    check(r2, { "daily 2xx": (r) => r.status >= 200 && r.status < 300 });
  });

  group("transactions", () => {
    const r3 = http.get(`${BASE_URL}/api/transactions?page=0&size=20`, params);
    check(r3, { "tx list 200": (r) => r.status === 200 });
  });

  sleep(1); // think-time — 실제 사용자 클릭 간격 근사
}

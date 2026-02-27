// Import the http module to make HTTP requests. From this point, you can use `http` methods to make HTTP requests.
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  // Key configurations for avg load test in this section
  scenarios: {
    rps_test: {
      executor: 'constant-arrival-rate', // 일정한 RPS 유지
      rate: 30, // 초당 RPS
      timeUnit: '1s', // RPS 측정 단위 (ex. 1초당 요청 수)
      duration: '2m', // 테스트 지속 시간 (2분)
      preAllocatedVUs: 200, // 미리 확보해 둘 가상 사용자 수 (필요 VU ≈ RPS × 평균 응답시간(초)) // 30 × 1 ≈ 30 → 여유 포함
      maxVUs: 400, // 최대 가상 사용자 수 (preAllocatedVUs에서 부족하면 확장)
    },
  },
};

export default () => {
  const payload = {
    stockCode: "005930",
    offerPrice: 70000,
    offerCnt: 10,
    offerSide: "BUY"
  };

  const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
  const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'; // 환경 변수가 설정되지 않은 경우 기본값 사용
  const res = http.post(`${BASE_URL}/offer/sync`, payload, { headers });

  check(res, {
    'Post status is 201': (r) => r.status === 201,
  });
};
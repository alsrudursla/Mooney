// Import the http module to make HTTP requests. From this point, you can use `http` methods to make HTTP requests.
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  // Key configurations for avg load test in this section
  scenarios: {
    rps_test: {
      executor: 'constant-arrival-rate', // 일정한 RPS 유지
      rate: 40, // 초당 RPS
      timeUnit: '1s', // RPS 측정 단위 (ex. 1초당 요청 수)
      duration: '2m', // 테스트 지속 시간 (2분)
      preAllocatedVUs: 800, // 미리 확보해 둘 가상 사용자 수 (필요 VU ≈ RPS × 평균 응답시간(초)) // 40 × 18 ≈ 720 → 여유 포함
      maxVUs: 1000, // 최대 가상 사용자 수 (preAllocatedVUs에서 부족하면 확장)
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
  const res = http.post('http://localhost:8080/offer/sync', payload, { headers });

  check(res, {
    'Post status is 201': (r) => r.status === 201,
  });
};
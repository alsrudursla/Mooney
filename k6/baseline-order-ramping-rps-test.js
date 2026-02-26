// Import the http module to make HTTP requests. From this point, you can use `http` methods to make HTTP requests.
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  // Key configurations for avg load test in this section
  scenarios: {
    rps_test: {
      executor: 'ramping-arrival-rate', // RPS를 단계적으로 올림 (cf. constant-arrival-rate : 일정한 RPS 유지)
      timeUnit: '1s', // RPS 측정 단위 (ex. 1초당 요청 수)
      preAllocatedVUs: 200, // 미리 확보해 둘 가상 사용자 수 (필요 VU ≈ RPS × 평균 응답시간(초))
      maxVUs: 1000, // 최대 가상 사용자 수 (preAllocatedVUs에서 부족하면 확장)

      stages: [
        { target: 100, duration: '1m' }, // 1분 동안 100 RPS 유지
        { target: 300, duration: '1m' }, // 1분 동안 300 RPS 유지
        { target: 500, duration: '1m' }, // 1분 동안 500 RPS 유지
      ],
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
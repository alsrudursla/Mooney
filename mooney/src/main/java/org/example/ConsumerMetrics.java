package org.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ConsumerMetrics {

    private final AtomicLong totalTime = new AtomicLong();
    private final AtomicLong count = new AtomicLong();
    private final AtomicLong maxTime = new AtomicLong();
    private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong totalMessageCount = new AtomicLong();

    // DB 시간 측정 메소드 추가
    private final AtomicLong totalDBTime = new AtomicLong();
    private final AtomicLong dbCount = new AtomicLong();

    // 메세지 1개씩 처리할 때
    public void record(long duration) {

        totalTime.addAndGet(duration);
        long currentCount = count.incrementAndGet();
        totalMessageCount.incrementAndGet();

        maxTime.updateAndGet(v -> Math.max(v, duration));
        minTime.updateAndGet(v -> Math.min(v, duration));

        if (currentCount % 1000 == 0) {
            log.info(
                "Consumer Stats - count: {}, avg: {} ms, max: {} ms, min: {} ms",
                currentCount,
                totalTime.get() / currentCount,
                maxTime.get(),
                minTime.get()
            );
        }
    }

    // 배치 처리할 때 (100개씩 처리)
    public void recordBatch(long batchDuration, int batchSize) {

        // 1. 배치 전체 시간 누적
        totalTime.addAndGet(batchDuration);
        long currentBatchCount = count.incrementAndGet(); // 배치 횟수 증가
        totalMessageCount.addAndGet(batchSize); // 배치 내 메세지 수 누적

        maxTime.updateAndGet(v -> Math.max(v, batchDuration));
        minTime.updateAndGet(v -> Math.min(v, batchDuration));

        if (currentBatchCount % 100 == 0) {
            // 2. 평균 배치 시간 계산
            double avgBatch = (double) totalTime.get() / currentBatchCount;
            
            // 3. 평균 메세지당 시간 계산
            double avgMessage = (double) batchDuration / batchSize;

            log.info(
                "Consumer Stats - batches: {}, avgBatch: {} ms, avgPerMessage: {} ms, maxBatch: {} ms, minBatch: {} ms",
                currentBatchCount,
                avgBatch,
                avgMessage,
                maxTime.get(),
                minTime.get()
            );
        }
    }

    // DB 시간 측정 메소드 (EntityManager 사용)
    public void recordDB(long dbDuration, int messageCount) {
        totalDBTime.addAndGet(dbDuration);
        dbCount.addAndGet(messageCount);
    }

    // 최종 통계 출력 (나머지 처리까지 포함)
    @PreDestroy
    public void printFinalStats() {
        if (count.get() == 0) return;

        // 배치 전체 평균 시간
        double avgBatch = (double) totalTime.get() / count.get();

        // 메세지당 평균 시간 (총 시간 / 총 메세지 수)
        double avgMessage = (double) totalTime.get() / totalMessageCount.get();

        // 초당 처리량
        double throughput = (double) totalMessageCount.get() / (totalTime.get() / 1000.0);

        // 메세지당 평균 DB 시간 (총 DB 시간 / 총 메세지 수)
        double avgDBTime = (double) totalDBTime.get() / dbCount.get();

        log.info(
            "--- FINAL PROCESSING STATS ---" +
            "\nTotal Operations: {}" +
            "\nTotal Messages: {}" +
            "\nAvg Time Per Operation (Total Time / Operation Count): {} ms" +
            "\nAvg Time Per Message (Total Time / Total Messages): {} ms" + 
            "\nMax Operation Time: {} ms" +
            "\nMin Operation Time: {} ms" +
            "\nThroughput: {} messages/sec" +
            "\nAvg DB Time Per Message (Total DB Time / Total Messages): {} ms",
            count.get(),
            totalMessageCount.get(),
            avgBatch,
            avgMessage,
            maxTime.get(),
            minTime.get(),
            throughput,
            avgDBTime
        );
    }
}

package org.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ConsumerMetrics {

    private final AtomicLong totalTime = new AtomicLong();
    private final AtomicLong count = new AtomicLong();
    private final AtomicLong maxTime = new AtomicLong();
    private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
    
    public void record(long duration) {

        totalTime.addAndGet(duration);
        count.incrementAndGet();

        maxTime.updateAndGet(v -> Math.max(v, duration));
        minTime.updateAndGet(v -> Math.min(v, duration));

        if (count.get() % 1000 == 0) {
            log.info(
                "📊 Consumer Stats - count: {}, avg: {} ms, max: {} ms, min: {} ms",
                count.get(),
                totalTime.get() / count.get(),
                maxTime.get(),
                minTime.get()
            );
        }
    }
}

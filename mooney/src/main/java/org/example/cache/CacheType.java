package org.example.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    STOCK("stock", 60 * 60, 10000), // 1시간 동안 캐시 유지
    USER("user", 60 * 60, 10000);

    private final String cacheName;
    private final int expiredAfterWrite;
    private final int maximumSize;
}

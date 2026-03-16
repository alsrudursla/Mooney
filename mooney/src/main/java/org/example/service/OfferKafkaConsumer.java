package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.dto.OfferDto;
import org.example.entity.Account;
import org.example.entity.Offer;
import org.example.entity.Stock;
import org.example.entity.User;
import org.example.ConsumerMetrics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferKafkaConsumer {

    private final StockService stockService;
    private final UserService userService;
    private final AccountService accountService;
    private final OfferService offerService;
    private final TradeService tradeService;
    
    // 성능 측정용 메트릭 클래스 주입
    private final ConsumerMetrics consumerMetrics;

    @Transactional
    @KafkaListener(topics = "order-request", groupId = "mooney-offer-group")
    //@KafkaListener(topics = "order-request", groupId = "mooney-offer-group", concurrency = "3")
    public void saveOffer(OfferDto dto) {
        log.debug("📤 메세지 수신 : {} {} {} {}", 
                dto.getStockCode(), dto.getOfferPrice(), dto.getOfferCnt(), dto.getOfferSide());

        // 성능 측정 시작
        long startTime = System.nanoTime();

        Stock stock = stockService.getStockByCode(dto.getStockCode());

        // TODO: [SECURITY] 테스트 목적으로만 사용되며, 프로덕션에서는 실제 인증된 사용자 ID로 대체해야 합니다.
        User user = userService.getUserById(1L);
        
        Account account = accountService.getAccountByUser(user);

        // 1. 주문 테이블에 저장
        Offer offer = offerService.createOffer(dto, stock, account);

        // 2. 거래 테이블에 저장 (PENDING 상태)
        tradeService.createTrade(offer);

        // 성능 측정 종료 및 기록
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        consumerMetrics.record(duration);
    }
}

package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.dto.OfferDto;
import org.example.entity.Account;
import org.example.entity.Offer;
import org.example.entity.Stock;
import org.example.entity.User;
import org.example.repository.*;
import org.example.ConsumerMetrics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferKafkaConsumer {

    private final StockRepository stockRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final TradeRepository tradeRepository;
    
    // 성능 측정용 메트릭 클래스 주입
    private final ConsumerMetrics consumerMetrics;

    @Transactional
    @KafkaListener(topics = "order-request", groupId = "mooney-offer-group")
    //@KafkaListener(topics = "order-request", groupId = "mooney-offer-group", concurrency = "3")
    public void saveOffer(OfferDto dto) {
        //log.info("📤 메세지 수신 : {} {} {} {}", 
        //        dto.getStockCode(), dto.getOfferPrice(), dto.getOfferCnt(), dto.getOfferSide());

        // 성능 측정 시작
        long startTime = System.nanoTime();

        Stock stock = Optional.ofNullable(stockRepository.findByStockCode(dto.getStockCode()))
                .orElseThrow(() -> new IllegalArgumentException("Stock with code " + dto.getStockCode() + " not found."));

        // TODO: [SECURITY] 테스트 목적으로만 사용되며, 프로덕션에서는 실제 인증된 사용자 ID로 대체해야 합니다.
        Optional<User> userOptional = userRepository.findById(1L);
        User user = userOptional.orElseThrow(() -> new IllegalArgumentException("User with ID 1L not found."));
        
        Account account = Optional.ofNullable(accountRepository.findByUser(user))
                .orElseThrow(() -> new IllegalStateException("Account for user ID 1L not found."));

        // 1. 주문 테이블에 저장
        Offer offer = dto.toEntity(dto, stock, account);
        offerRepository.save(offer);

        // 2. 체결 테이블에 저장 (PENDING 상태)
        tradeRepository.save(dto.addTradeEntity(offer));

        // 성능 측정 종료 및 기록
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        consumerMetrics.record(duration);
    }
}

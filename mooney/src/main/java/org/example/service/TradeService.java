package org.example.service;

import org.example.entity.Offer;
import org.example.entity.Trade;
import org.example.repository.TradeRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    // 거래 테이블에 저장 (PENDING 상태)
    @Transactional
    public Trade createTrade(Offer offer) {
        Trade trade = Trade.builder()
                .offer(offer)
                .build();

        return tradeRepository.save(trade);
    }
    
}

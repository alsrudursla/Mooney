package org.example.service;

import java.util.List;

import org.example.entity.Offer;
import org.example.entity.Trade;
import org.example.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 거래 테이블에 저장 (배치용)
    public Trade createTrade4Batch(Offer offer) {
        Trade trade = Trade.builder()
                .offer(offer)
                .build();

        return trade;
    }

    // 거래 테이블에 한 번에 저장
    @Transactional
    public void saveAll(List<Trade> trades) {
        tradeRepository.saveAll(trades);
    }
    
}

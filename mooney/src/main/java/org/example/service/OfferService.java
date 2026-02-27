package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Account;
import org.example.entity.Offer;
import org.example.entity.Trade;
import org.example.repository.OfferRepository;
import org.example.repository.TradeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "websocket.enabled", // 1. application.yml에서 'websocket.enabled' 속성을 찾는다.
    havingValue = "true", // 2. 그 값이 "true"일 때만 이 클래스를 활성화한다.
    matchIfMissing = false // 3. 만약 속성이 없으면 (or 기본값) 비활성화한다.
)
public class OfferService {
    private final OfferRepository offerRepository;
    private final TradeRepository tradeRepository;

    // PENDING 중인 주문의 stockCode 조회
    @Transactional(readOnly = true)
    public List<String> getPendingStockCodes() {
        return offerRepository.findDistinctStockCodesByOfferStatus("PENDING");
    }

    // 호가랑 체결가가 매칭하는지 확인
    @Transactional
    public void matchOrders(String stockCode, double currentPrice) {
        List<Offer> pendingOffers = offerRepository.findByStock_StockCodeAndOfferStatus(stockCode, "PENDING");

        for (Offer offer : pendingOffers) {
            boolean fill = false;

            if (offer.getOfferSide().equals("BUY") && offer.getOfferPrice() == currentPrice) {
                fill = true;
            } else if (offer.getOfferSide().equals("SELL") && offer.getOfferPrice() == currentPrice) {
                fill = true;
            }

            if (fill) {
                offer.isFilled();
                Account account = offer.getAccount();
                Trade trade = Trade.builder()
                        .offer(offer)
                        .build();

                tradeRepository.save(trade);

                if (offer.getOfferSide().equals("BUY")) {
                    account.updateCashBalance(account.getCashBalance() - offer.getOfferPrice() * offer.getOfferCnt());
                } else if (offer.getOfferSide().equals("SELL")) {
                    account.updateCashBalance(account.getCashBalance() + offer.getOfferPrice() * offer.getOfferCnt() );
                }

            }
        }
    }
}
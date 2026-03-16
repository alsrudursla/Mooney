package org.example.service;

import org.example.dto.OfferDto;
import org.example.entity.Account;
import org.example.entity.Offer;
import org.example.entity.Stock;
import org.example.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyncOfferService {
    private final StockService stockService;
    private final UserService userService;
    private final AccountService accountService;
    private final OfferService offerService;
    private final TradeService tradeService;

    @Transactional
    public void saveOffer(OfferDto dto) {
        // 기존 리스너에 있던 로직 그대로!
        Stock stock = stockService.getStockByCode(dto.getStockCode());

        // TODO: [SECURITY] 테스트 목적으로만 사용되며, 프로덕션에서는 실제 인증된 사용자 ID로 대체해야 합니다.
        User user = userService.getUserById(1L);
        
        Account account = accountService.getAccountByUser(user);

        // 1. 주문 테이블에 저장
        Offer offer = offerService.createOffer(dto, stock, account);

        // 2. 거래 테이블에 저장 (PENDING 상태)
        tradeService.createTrade(offer);
    }
}
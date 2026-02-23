package org.example.service;

import java.util.Optional;

import org.example.dto.OfferDto;
import org.example.entity.Account;
import org.example.entity.Offer;
import org.example.entity.Stock;
import org.example.entity.User;
import org.example.repository.AccountRepository;
import org.example.repository.OfferRepository;
import org.example.repository.StockRepository;
import org.example.repository.TradeRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyncOfferService {
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final OfferRepository offerRepository;
    private final TradeRepository tradeRepository;

    @Transactional
    public void saveOffer(OfferDto dto) {
        // 기존 리스너에 있던 로직 그대로!
        // Stock, Account 데이터 조회
        Stock stock = stockRepository.findByStockCode(dto.getStockCode());
        Optional<User> user = userRepository.findById(1L); // 나중에 인증 정보로 변경 가능
        Account account = accountRepository.findByUser(user);

        // 1. 주문 테이블에 저장
        Offer offer = dto.toEntity(dto, stock, account);
        offerRepository.save(offer);

        // 2. 체결 테이블에 저장 (PENDING 상태)
        tradeRepository.save(dto.addTradeEntity(offer));
    }
}
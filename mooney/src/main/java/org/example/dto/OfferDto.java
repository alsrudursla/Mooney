package org.example.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.Account;
import org.example.entity.Offer;
import org.example.entity.Stock;
import org.example.entity.Trade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
@NoArgsConstructor
public class OfferDto {
    // 종목코드
    @NotBlank(message = "Stock code must not be blank")
    private String stockCode;

    // 호가 (주문가)
    @Positive(message = "Offer price must be positive")
    private double offerPrice;

    // 주문량
    @Positive(message = "Offer count must be positive")
    private int offerCnt;

    // 매도 or 매수 여부
    @NotBlank(message = "Offer side must not be blank")
    private String offerSide;

    @Builder(builderMethodName = "offerBuilder")
    public Offer toEntity(OfferDto dto, Stock stock, Account account) {
        return Offer.builder()
                .stock(stock)
                .offerPrice(dto.getOfferPrice())
                .offerCnt(dto.getOfferCnt())
                .offerSide(dto.getOfferSide())
                .offerStatus("PENDING")
                .account(account)
                .build();
    }

    @Builder(builderMethodName = "tradeBuilder")
    public Trade addTradeEntity(Offer offer) {
        return Trade.builder()
                .offer(offer)
                .build();
    }
}

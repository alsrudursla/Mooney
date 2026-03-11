package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.dto.OfferDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/offer")
@RequiredArgsConstructor
@Slf4j
public class OfferController {

    private final KafkaTemplate<String, OfferDto> kafkaTemplate;

    @PostMapping()
    public ResponseEntity<?> offerStockSync(@Valid @ModelAttribute OfferDto dto) {
        // @ModelAttribute → Thymeleaf 폼 데이터를 DTO로 자동 매핑
        // DTO의 필드와 폼 input name이 일치하면 자동 매핑
        try {
            kafkaTemplate.send("order-request", dto);
            log.debug("📤 메세지 발행 : {} {} {} {}", 
                dto.getStockCode(), dto.getOfferPrice(), dto.getOfferCnt(), dto.getOfferSide());
            return ResponseEntity.status(HttpStatus.CREATED).body("주문이 접수되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

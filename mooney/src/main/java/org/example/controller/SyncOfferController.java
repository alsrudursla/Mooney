package org.example.controller;

import org.example.dto.OfferDto;
import org.example.service.SyncOfferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/offer/sync")
@RequiredArgsConstructor
public class SyncOfferController {
    private final SyncOfferService syncOfferService;

    @PostMapping()
    public ResponseEntity<?> offerStockSync(@Valid @ModelAttribute OfferDto dto) {
        // DB에 직접 저장 (동기 방식)
        // 이 과정에서 DB 커넥션을 점유하고 저장이 완료될 때까지 쓰레드가 대기하게 됩니다.
        try {
            syncOfferService.saveOffer(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("주문이 접수되었습니다.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

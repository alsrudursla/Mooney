package org.example.service;

import org.example.entity.Stock;
import org.example.repository.StockRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Cacheable(value="stock", key="#p0", condition="#p0!=null")
    public Stock getStockByCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode);
    }
    
}

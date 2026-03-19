package org.example.service;

import org.example.entity.Account;
import org.example.entity.User;
import org.example.repository.AccountRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Account getAccountByUser(User user) {
        return accountRepository.findByUser(user);
    }
    
}

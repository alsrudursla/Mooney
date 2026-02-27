package org.example.repository;

import org.example.entity.Account;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByUser(User user);
}

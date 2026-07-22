package com.leadgen.bot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, Long> {
    List<TelegramAccount> findByStatus(String status);
}

package com.leadgen.bot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActiveDialogSummaryRepository extends JpaRepository<ActiveDialogSummary, Long> {
    List<ActiveDialogSummary> findByStatus(String status);
}

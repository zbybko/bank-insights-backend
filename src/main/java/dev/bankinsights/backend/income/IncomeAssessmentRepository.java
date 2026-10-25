package dev.bankinsights.backend.income;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IncomeAssessmentRepository extends JpaRepository<IncomeAssessment, UUID> {
    @Query("""
        select a from IncomeAssessment a join fetch a.transaction t
        where a.source = :source
          and a.type <> dev.bankinsights.backend.income.IncomeType.UNKNOWN
          and lower(trim(t.description)) = lower(trim(:description))
          and t.currency = :currency and t.state = 'COMPLETED' and t.amount > 0
          and t.bookingDate < :before
        order by t.bookingDate desc, t.id desc
        """)
    List<IncomeAssessment> findConfirmedHistory(IncomeAssessment.Source source, String description, String currency, LocalDateTime before,
                                               Pageable pageable);
}

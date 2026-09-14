package com.dazbones.repository;
import com.dazbones.model.AnnualFee;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface AnnualFeeRepository extends JpaRepository<AnnualFee,Long> {
    Optional<AnnualFee> findByPlayerIdAndFiscalYear(Long playerId,Integer fiscalYear);
    List<AnnualFee> findByFiscalYearOrderByPlayerIdAsc(Integer year);
    List<AnnualFee> findByPlayerIdOrderByFiscalYearDesc(Long playerId);
    @Query("select distinct f.fiscalYear from AnnualFee f order by f.fiscalYear desc") List<Integer> years();
    @Query("select count(f) from AnnualFee f where f.fiscalYear=:year and f.amount>f.paidAmount") long countUnpaid(int year);
}

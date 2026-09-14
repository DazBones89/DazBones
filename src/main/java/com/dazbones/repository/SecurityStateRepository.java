package com.dazbones.repository;
import com.dazbones.model.SecurityState;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface SecurityStateRepository extends JpaRepository<SecurityState,Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SecurityState s where s.id=1")
    SecurityState lockState();
}

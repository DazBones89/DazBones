package com.dazbones.repository;
import com.dazbones.model.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditEntryRepository extends JpaRepository<AuditEntry,Long> {}

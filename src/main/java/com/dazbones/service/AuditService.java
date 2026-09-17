package com.dazbones.service;
import com.dazbones.model.*;
import com.dazbones.repository.AuditEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class AuditService {
    private final AuditEntryRepository repository;
    public AuditService(AuditEntryRepository repository){this.repository=repository;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void record(UserSession user,String target,String outcome,int status){repository.saveAndFlush(new AuditEntry(user,target,outcome,status));}
}

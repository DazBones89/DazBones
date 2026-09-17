package com.dazbones.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="audit_entries")
public class AuditEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private LocalDateTime occurredAt;
    @Column(nullable=false,length=64) private String loginId;
    @Column(nullable=false,length=16) private String role;
    @Column(nullable=false,length=8) private String method;
    @Column(nullable=false,length=255) private String target;
    @Column(nullable=false,length=32) private String outcome;
    @Column(nullable=false) private int httpStatus;
    protected AuditEntry() {}
    public AuditEntry(UserSession user, String target, String outcome, int status) {
        this.occurredAt=LocalDateTime.now(); this.loginId=user.getLoginId(); this.role=user.getRole();
        this.method="POST"; this.target=target; this.outcome=outcome; this.httpStatus=status;
    }
    public Long getId(){return id;}
    public LocalDateTime getOccurredAt(){return occurredAt;}
    public String getLoginId(){return loginId;}
    public String getRole(){return role;}
    public String getMethod(){return method;}
    public String getTarget(){return target;}
    public String getOutcome(){return outcome;}
    public int getHttpStatus(){return httpStatus;}
}

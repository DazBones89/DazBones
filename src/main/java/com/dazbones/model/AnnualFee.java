package com.dazbones.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="annual_fees",uniqueConstraints=@UniqueConstraint(columnNames={"player_id","fiscal_year"}))
public class AnnualFee {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="player_id",nullable=false) private Long playerId;
    @Column(name="fiscal_year",nullable=false) private Integer fiscalYear;
    @Column(nullable=false) private Integer amount=0;
    @Column(nullable=false) private boolean paid;
    public boolean isPaid(){return paid;} public void setPaid(boolean value){paid=value;}
    @Column(name="paid_amount",nullable=false) private Integer paidAmount=0;
    @Column(length=1000) private String comment;
    @Version private Long version;
    @Column(name="created_at") private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @PrePersist void create(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;}
    public Integer getFiscalYear(){return fiscalYear;} public void setFiscalYear(Integer v){fiscalYear=v;}
    public Integer getAmount(){return amount;} public void setAmount(Integer v){amount=v;}
    public Integer getPaidAmount(){return paidAmount;} public void setPaidAmount(Integer v){paidAmount=v;}
    public String getComment(){return comment;} public void setComment(String v){comment=v;}
    public Long getVersion(){return version;}
    public int getUnpaidAmount(){return amount-paidAmount;}
}

package com.dazbones.form;
import jakarta.validation.constraints.*;
public class AnnualFeeForm {
    @NotNull private Long playerId;
    @NotNull @Min(1900) @Max(2100) private Integer fiscalYear;
    @NotNull @Min(0) @Max(1000000000) private Integer amount;
    @NotNull @Min(0) @Max(1000000000) private Integer paidAmount;
    @Size(max=1000) private String comment;
    private Long version;
    @AssertTrue(message="入金額は請求額以下で入力してください")
    public boolean isPaymentValid(){return amount==null || paidAmount==null || paidAmount<=amount;}
    public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;}
    public Integer getFiscalYear(){return fiscalYear;} public void setFiscalYear(Integer v){fiscalYear=v;}
    public Integer getAmount(){return amount;} public void setAmount(Integer v){amount=v;}
    public Integer getPaidAmount(){return paidAmount;} public void setPaidAmount(Integer v){paidAmount=v;}
    public String getComment(){return comment;} public void setComment(String v){comment=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}

package com.dazbones.service;
import com.dazbones.model.*;
import com.dazbones.form.AnnualFeeForm;
import com.dazbones.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class AnnualFeeService {
    public record Totals(long amount,long paidAmount){public long getAmount(){return amount;} public long getPaidAmount(){return paidAmount;} public long getUnpaidAmount(){return amount-paidAmount;}}
    private final AnnualFeeRepository repo;
    private final PlayerRepository players;
    public AnnualFeeService(AnnualFeeRepository repo,PlayerRepository players){this.repo=repo;this.players=players;}
    public List<AnnualFee> year(int year){return repo.findByFiscalYearOrderByPlayerIdAsc(year);}
    public List<AnnualFee> player(Long id){return repo.findByPlayerIdOrderByFiscalYearDesc(id);}
    public List<Integer> years(){var years=new TreeSet<Integer>(Comparator.reverseOrder());years.add(java.time.LocalDate.now().getYear());years.addAll(repo.years());return new ArrayList<>(years);}
    public Totals totals(List<AnnualFee> rows){return new Totals(rows.stream().mapToLong(AnnualFee::getAmount).sum(),rows.stream().mapToLong(AnnualFee::getPaidAmount).sum());}
    public Totals team(){return totals(repo.findAll());}
    public long unpaidCount(){var paid=year(java.time.LocalDate.now().getYear()).stream().filter(AnnualFee::isPaid).map(AnnualFee::getPlayerId).collect(java.util.stream.Collectors.toSet());return players.findActivePlayers().stream().filter(p->!paid.contains(p.getId())).count();}
    @Transactional
    public void save(AnnualFeeForm form){
        var player=players.lockForFee(form.getPlayerId()).orElseThrow(()->new IllegalArgumentException("選手が見つかりません"));
        var row=repo.findByPlayerIdAndFiscalYear(form.getPlayerId(),form.getFiscalYear()).orElse(null);
        if(row==null && !Integer.valueOf(0).equals(player.getDeleteFlg())) throw new IllegalArgumentException("退部者の新規請求は登録できません");
        if(row!=null && !Objects.equals(row.getVersion(),form.getVersion())) throw new IllegalArgumentException("別の操作で更新されています。画面を再読込して確認してください");
        if(row==null && form.getVersion()!=null) throw new IllegalArgumentException("更新対象が見つかりません");
        if(row==null) row=new AnnualFee();
        row.setPlayerId(form.getPlayerId());row.setFiscalYear(form.getFiscalYear());row.setAmount(form.getAmount());
        row.setPaidAmount(form.getPaidAmount());row.setComment(form.getComment());repo.saveAndFlush(row);
    }
}

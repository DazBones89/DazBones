package com.dazbones.service;
import com.dazbones.model.*;
import com.dazbones.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class InputService {
 private final PlayerRepository players; private final AnnualFeeRepository fees; private final GearRepository gears;
 private final AttendanceService attendance; private final AttendanceDateRepository dates; private final PlayerVisibility visibility;
 private final SecurityStateRepository states;
 public InputService(PlayerRepository p,AnnualFeeRepository f,GearRepository g,AttendanceService a,AttendanceDateRepository d,PlayerVisibility v,SecurityStateRepository s){players=p;fees=f;gears=g;attendance=a;dates=d;visibility=v;states=s;}
 private void allowed(UserSession u){if(u==null||!u.canManage())throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 private void year(int y){if(y<1900||y>2100)throw new IllegalArgumentException("年度は1900〜2100です");}
 private void version(Long actual,Long expected){if(!Objects.equals(actual==null?-1L:actual,expected))throw new ResponseStatusException(HttpStatus.CONFLICT,"他の画面で更新されました。入力を控えて再読み込みしてください");}
 private Player active(Long id){return players.lockForFee(id).filter(p->Integer.valueOf(0).equals(p.getDeleteFlg())).orElseThrow(()->new IllegalArgumentException("表示中の選手を指定してください"));}
 @Transactional(readOnly=true)
 public Map<String,Object> data(UserSession u,int year,String month){
  allowed(u);year(year);YearMonth m=attendance.month(month);var roster=players.findActivePlayers();var fields=visibility.fields(u);
  List<Map<String,Object>> ps=new ArrayList<>();
  for(var p:roster){var r=new LinkedHashMap<String,Object>();r.put("id",p.getId());r.put("name",fields.get("name")?p.getName():"選手 #"+p.getId());r.put("version",p.getVersion());
   if(fields.get("atBats"))r.put("atBats",p.getAtBats());if(fields.get("hits"))r.put("hits",p.getHits());if(fields.get("average")&&fields.get("atBats")&&fields.get("hits"))r.put("average",p.getDisplayAverage());ps.add(r);}
  var fs=new ArrayList<Map<String,Object>>(); for(var f:fees.findByFiscalYearOrderByPlayerIdAsc(year))fs.add(Map.of("playerId",f.getPlayerId(),"paid",f.isPaid(),"comment",Objects.toString(f.getComment(),""),"version",f.getVersion()));
  var gs=new ArrayList<Map<String,Object>>();for(var g:gears.findAll()){var r=new LinkedHashMap<String,Object>();r.put("id",g.getId());r.put("name",g.getName());r.put("ownerId",g.getOwnerId());r.put("comment",Objects.toString(g.getComment(),""));r.put("version",g.getVersion());gs.add(r);}
  var as=new ArrayList<Map<String,Object>>();var ids=roster.stream().map(Player::getId).toList();
  for(var a:attendance.answers(m))if(ids.contains(a.getPlayerId()))as.add(Map.of("playerId",a.getPlayerId(),"date",a.getTargetDate().toString(),"status",a.getStatus(),"memo",Objects.toString(a.getMemo(),""),"version",a.getVersion()));
  return Map.of("players",ps,"fields",fields,"fees",fs,"gears",gs,"dates",attendance.dates(m).stream().map(LocalDate::toString).toList(),"answers",as,"year",year,"month",m.toString());
 }
 @Transactional public Map<String,Object> stats(UserSession u,Long id,Integer atBats,Integer hits,Long expected){
  allowed(u);states.lockState();var p=active(id);version(p.getVersion(),expected);
  if(atBats!=null){if(!visibility.visible("atBats",u))throw new ResponseStatusException(HttpStatus.FORBIDDEN);p.setAtBats(atBats);}
  if(hits!=null){if(!visibility.visible("hits",u))throw new ResponseStatusException(HttpStatus.FORBIDDEN);p.setHits(hits);}
  if(visibility.visible("atBats",u)&&atBats==null || visibility.visible("hits",u)&&hits==null)throw new IllegalArgumentException("表示中の打数・安打を入力してください");
  int bats=Objects.requireNonNullElse(p.getAtBats(),0),hitCount=Objects.requireNonNullElse(p.getHits(),0);
  if(bats<0||hitCount<0||hitCount>bats)throw new IllegalArgumentException("打数・安打は0以上、安打は打数以下で入力してください");
  players.saveAndFlush(p);return Map.of("version",p.getVersion());
 }
 @Transactional public Map<String,Object> fee(UserSession u,Long id,int year,boolean paid,String comment,Long expected){
  allowed(u);year(year);states.lockState();active(id);if(comment==null||comment.length()>1000)throw new IllegalArgumentException("コメントは1000文字以内です");
  var f=fees.findByPlayerIdAndFiscalYear(id,year).orElse(null);version(f==null?null:f.getVersion(),expected);
  if(f==null){f=new AnnualFee();f.setPlayerId(id);f.setFiscalYear(year);}f.setPaid(paid);f.setComment(comment);fees.saveAndFlush(f);return Map.of("version",f.getVersion());
 }
 @Transactional public Map<String,Object> gear(UserSession u,Long id,String name,Long owner,String comment,Long expected,boolean delete){
  allowed(u);states.lockState();var g=id==null?null:gears.findById(id).orElseThrow(()->new IllegalArgumentException("道具がありません"));version(g==null?null:g.getVersion(),expected);
  if(delete){if(g==null)throw new IllegalArgumentException("道具を指定してください");gears.delete(g);return Map.of("deleted",true);}
  if(name==null||name.isBlank()||name.length()>100||comment==null||comment.length()>1000)throw new IllegalArgumentException("道具名は1〜100文字、コメントは1000文字以内です");
  if(owner!=null && (g==null||!Objects.equals(g.getOwnerId(),owner)))active(owner);
  if(g==null)g=new Gear();g.setName(name.trim());g.setOwnerId(owner);g.setComment(comment);gears.saveAndFlush(g);return Map.of("id",g.getId(),"version",g.getVersion());
 }
 @Transactional public void date(UserSession u,LocalDate date){allowed(u);year(date.getYear());states.lockState();dates.saveAndFlush(new AttendanceDate(date));}
 @Transactional public void visible(UserSession u,Long id,boolean visible){allowed(u);if(!u.isMaster())throw new ResponseStatusException(HttpStatus.FORBIDDEN);states.lockState();var p=players.lockForFee(id).orElseThrow(()->new IllegalArgumentException("選手が見つかりません"));p.setDeleteFlg(visible?0:1);p.setDeletedAt(visible?null:LocalDateTime.now());p.setUpdatedAt(LocalDateTime.now());players.saveAndFlush(p);}
}

package com.dazbones.service;
import com.dazbones.model.*;
import com.dazbones.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PlayerVisibility {
 public static final Map<String,String> FIELDS;
 static {var m=new LinkedHashMap<String,String>();m.put("name","名前");m.put("backNumber","背番号");m.put("positions","守備位置");m.put("throwHand","投げ");m.put("batHand","打ち");m.put("image","写真");m.put("comment","コメント");m.put("atBats","打数");m.put("hits","安打");m.put("average","打率");FIELDS=Collections.unmodifiableMap(m);}
 private final SiteSettingRepository repo; private final SecurityStateRepository states;
 public PlayerVisibility(SiteSettingRepository repo,SecurityStateRepository states){this.repo=repo;this.states=states;}
 public boolean visible(String field,UserSession user){return user!=null&&user.isMaster() || !repo.findById("player."+field).map(s->s.value.equals("hidden")).orElse(false);}
 public Map<String,Boolean> fields(UserSession user){var m=new LinkedHashMap<String,Boolean>();FIELDS.keySet().forEach(k->m.put(k,visible(k,user)));return m;}
 @Transactional public void set(String field,boolean visible){states.lockState();if(!FIELDS.containsKey(field))throw new IllegalArgumentException("項目を確認してください");repo.save(new SiteSetting("player."+field,visible?"visible":"hidden"));}
 public String label(Player p,UserSession user){return visible("name",user)?p.getName():"選手 #"+p.getId();}
}

package com.dazbones.service;
import com.dazbones.model.SiteSetting;
import com.dazbones.repository.SiteSettingRepository;
import com.dazbones.repository.SecurityStateRepository;
import java.net.URI;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SocialService {
 private final SiteSettingRepository settings; private final SecurityStateRepository states;
 public SocialService(SiteSettingRepository settings,SecurityStateRepository states){this.settings=settings;this.states=states;}
 public record Link(String id,String url,String platform,String account){}
 private Link link(String id,String url){var u=URI.create(url);String host=u.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.","");String path=Objects.toString(u.getPath(),"").replaceAll("^/|/$","");String platform=host.equals("instagram.com")?"Instagram":host.equals("youtube.com")||host.equals("youtu.be")?"YouTube":host;
 return new Link(id,url,platform,platform.equals("Instagram")?"@"+path:path.isBlank()?host:path);}
 @Transactional(readOnly=true) public List<Link> links(){var rows=settings.findAll().stream().filter(s->s.key.startsWith("sns.")).sorted(Comparator.comparing(s->s.key)).map(s->link(s.key,s.value)).toList();
 if(!settings.existsById("sns-initialized")){var all=new ArrayList<Link>(rows);all.add(0,link("sns.default","https://www.instagram.com/dazbones89/"));return all;}return rows;}
 private void initialize(){if(!settings.existsById("sns-initialized")){settings.save(new SiteSetting("sns.default","https://www.instagram.com/dazbones89/"));settings.save(new SiteSetting("sns-initialized","true"));}}
 @Transactional public void add(String value){String url=value==null?"":value.trim();URI u;try{u=URI.create(url);}catch(Exception e){throw new IllegalArgumentException("正しいURLを入力してください");}
 if(url.length()>255||!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getPort()!=-1)throw new IllegalArgumentException("https://から始まるSNSのURLを255文字以内で入力してください");
 states.lockState();initialize();if(settings.findAll().stream().anyMatch(s->s.key.startsWith("sns.")&&s.value.equals(url)))return;settings.save(new SiteSetting("sns."+UUID.randomUUID(),url));}
 @Transactional public void delete(String id){if(id==null||!id.startsWith("sns."))throw new IllegalArgumentException("SNSを指定してください");states.lockState();initialize();settings.deleteById(id);}
}

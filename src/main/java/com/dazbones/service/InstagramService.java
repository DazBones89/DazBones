package com.dazbones.service;
import com.dazbones.model.*;
import com.dazbones.repository.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class InstagramService {
 private final InstagramPostRepository posts; private final SecurityStateRepository states;
 public InstagramService(InstagramPostRepository posts,SecurityStateRepository states){this.posts=posts;this.states=states;}
 private void member(UserSession u){if(u==null||!u.canManage())throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 private void master(UserSession u){if(u==null||!u.isMaster())throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 @Transactional(readOnly=true) public List<InstagramPost> ordered(){return posts.findAll().stream().sorted(Comparator.comparing(InstagramPost::getDisplayOrder,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(InstagramPost::getPostedAt,Comparator.reverseOrder()).thenComparing(InstagramPost::getShortcode,Comparator.reverseOrder())).toList();}
 public String revision(List<InstagramPost> rows){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rows.stream().map(p->p.getId()+":"+p.getDisplayOrder()).collect(java.util.stream.Collectors.joining(",")).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 @Transactional public void add(UserSession u,InstagramMetadataClient.PostUrl url,java.time.LocalDateTime date){member(u);states.lockState();if(posts.existsByShortcode(url.shortcode()))throw new IllegalArgumentException("この投稿は登録済みです。");var p=new InstagramPost();p.setShortcode(url.shortcode());p.setUrl(url.url());p.setPostedAt(date);posts.save(p);}
 @Transactional public void delete(UserSession u,List<Long> ids){member(u);if(ids==null||ids.isEmpty())throw new IllegalArgumentException("削除する投稿を選択してください。");states.lockState();posts.deleteAllById(ids);}
 @Transactional public void move(UserSession u,Long id,int direction,String expected){master(u);if(direction!=-1&&direction!=1)throw new IllegalArgumentException("移動方向を確認してください。");states.lockState();var rows=new ArrayList<>(ordered());if(!revision(rows).equals(expected))throw new IllegalArgumentException("一覧が更新されています。再読み込みして並び替えてください。");int index=-1;for(int i=0;i<rows.size();i++)if(rows.get(i).getId().equals(id))index=i;if(index<0)throw new IllegalArgumentException("投稿が見つかりません。");int target=index+direction;if(target<0||target>=rows.size())return;Collections.swap(rows,index,target);for(int i=0;i<rows.size();i++)rows.get(i).setDisplayOrder(i);posts.saveAll(rows);}
 @Transactional public void reset(UserSession u,String expected){master(u);states.lockState();var rows=ordered();if(!revision(rows).equals(expected))throw new IllegalArgumentException("一覧が更新されています。再読み込みしてください。");rows.forEach(p->p.setDisplayOrder(null));posts.saveAll(rows);}
 public void view(org.springframework.ui.Model model,int requested,boolean controls){var rows=ordered();int pages=Math.max(1,(rows.size()+5)/6);int page=Math.max(0,Math.min(requested,pages-1));model.addAttribute("instagramPosts",rows.subList(page*6,Math.min(rows.size(),page*6+6)));model.addAttribute("instagramTotal",rows.size());model.addAttribute("instagramPage",page);model.addAttribute("instagramPages",pages);model.addAttribute("instagramRevision",revision(rows));model.addAttribute("instagramControls",controls);model.addAttribute("instagramCustomOrder",rows.stream().anyMatch(p->p.getDisplayOrder()!=null));}
}

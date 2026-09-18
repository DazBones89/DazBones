package com.dazbones.service;

import com.dazbones.model.*;
import com.dazbones.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Service
public class CredentialService {
    private final LoginCredentialRepository credentials;
    private final SecurityStateRepository states;
    private final SurveyMemberRepository members;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String adminCode, editorCode;
    public CredentialService(LoginCredentialRepository credentials, SecurityStateRepository states,
            SurveyMemberRepository members, @Value("${app.auth.master-code:${app.auth.admin-code:}}") String adminCode,
            @Value("${app.auth.player-code:${app.auth.editor-code:}}") String editorCode) {
        this.credentials=credentials; this.states=states; this.members=members;
        this.adminCode=adminCode; this.editorCode=editorCode;
    }
    @jakarta.annotation.PostConstruct
    public void initialize() {
        if (!states.existsById(1)) states.save(new SecurityState());
        if (!adminCode.isBlank() && adminCode.equals(editorCode)) throw new IllegalStateException("管理者と編集者のコードを分けてください");
        seed("master",adminCode); seed("player",editorCode);
    }
    private void seed(String role,String code) {
        if (code.isBlank() || credentials.existsById(role)) return;
        if (code.getBytes(StandardCharsets.UTF_8).length>72) throw new IllegalStateException("コードは72バイト以内です");
        LoginCredential c=new LoginCredential(); c.setLoginId(role); c.setRole(role);
        c.setCodeHash(encoder.encode(code)); credentials.save(c);
    }
    @Transactional(readOnly=true)
    public UserSession authenticate(String loginId,String code) {
        if (code==null || code.isBlank() || code.getBytes(StandardCharsets.UTF_8).length>72) return null;
        List<String> ids = loginId==null || loginId.isBlank() ? List.of("master","player") : (List.of("master","player").contains(loginId.trim()) ? List.of(loginId.trim()) : List.of());
        for(String id:ids) {
            var c=credentials.findById(id).orElse(null);
            if(c!=null && encoder.matches(code,c.getCodeHash()) && memberActive(c.getMemberId()))
                return new UserSession(c.getRole(),c.getLoginId(),c.getMemberId(),generation(),c.getVersion());
        }
        return null;
    }
    public long generation(){return states.findById(1).orElseThrow().getGeneration();}
    private boolean memberActive(Long id) {
        return id==null || members.findById(id).map(m->Integer.valueOf(0).equals(m.getDeleteFlg())).orElse(false);
    }
    @Transactional(readOnly=true)
    public boolean isValid(UserSession user) {
        if(user==null || !user.canManage() || user.getLoginId()==null || user.getGeneration()!=generation()) return false;
        var c=credentials.findById(user.getLoginId()).orElse(null);
        return c!=null && Objects.equals(c.getVersion(),user.getCredentialVersion()) && memberActive(c.getMemberId());
    }
    @Transactional
    public String issueMemberCode(Long id) {
        var member=members.findById(id).orElseThrow(()->new IllegalArgumentException("回答者が見つかりません"));
        if(!Integer.valueOf(0).equals(member.getDeleteFlg())) throw new IllegalArgumentException("先に回答者を復元してください");
        byte[] random=new byte[18]; new SecureRandom().nextBytes(random);
        String code=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        var c=credentials.findById("member-"+id).orElse(new LoginCredential());
        c.setLoginId("member-"+id); c.setRole("member"); c.setMemberId(id); c.setCodeHash(encoder.encode(code));
        credentials.saveAndFlush(c);
        return code;
    }
    @Transactional
    public void changeAdminCode(String current,String next,String confirmation){ changeCode("master",current,next,confirmation); }
    @Transactional
    public void changeCode(String role,String current,String next,String confirmation) {
        if(!List.of("master","player").contains(role))throw new IllegalArgumentException("権限を確認してください");
        if(next==null || next.isBlank() || next.getBytes(StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("コードは1〜72バイトで入力してください");
        if(!next.equals(confirmation))throw new IllegalArgumentException("確認用コードが一致しません");
        SecurityState state=states.lockState();
        var master=credentials.findById("master").orElseThrow();
        if(current==null || !encoder.matches(current,master.getCodeHash()))throw new IllegalArgumentException("現在のmasterコードが違います");
        var other=credentials.findById(role.equals("master")?"player":"master").orElseThrow();
        if(encoder.matches(next,other.getCodeHash()))throw new IllegalArgumentException("役割ごとに異なるコードを設定してください");
        var c=credentials.findById(role).orElseThrow(); c.setCodeHash(encoder.encode(next));credentials.saveAndFlush(c);state.advance();states.save(state);
    }
}

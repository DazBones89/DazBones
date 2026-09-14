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
            SurveyMemberRepository members, @Value("${app.auth.admin-code:}") String adminCode,
            @Value("${app.auth.editor-code:}") String editorCode) {
        this.credentials=credentials; this.states=states; this.members=members;
        this.adminCode=adminCode; this.editorCode=editorCode;
    }
    @jakarta.annotation.PostConstruct
    public void initialize() {
        if (!states.existsById(1)) states.save(new SecurityState());
        if (!adminCode.isBlank() && adminCode.equals(editorCode)) throw new IllegalStateException("管理者と編集者のコードを分けてください");
        seed("admin",adminCode); seed("editor",editorCode);
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
        List<String> ids = loginId==null || loginId.isBlank() ? List.of("admin","editor") : List.of(loginId.trim());
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
        if(user==null || user.getLoginId()==null || user.getGeneration()!=generation()) return false;
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
    public void changeAdminCode(String current,String next,String confirmation) {
        if(next==null || next.length()<10 || next.isBlank() || next.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("新しいコードは10文字以上・72バイト以内で入力してください");
        if(!next.equals(confirmation)) throw new IllegalArgumentException("確認用コードが一致しません");
        SecurityState state=states.lockState();
        var c=credentials.findById("admin").orElseThrow();
        if(current==null || !encoder.matches(current,c.getCodeHash())) throw new IllegalArgumentException("現在のコードが違います");
        if(encoder.matches(next,c.getCodeHash())) throw new IllegalArgumentException("現在とは異なるコードを指定してください");
        var editor=credentials.findById("editor").orElse(null);
        if(editor!=null && encoder.matches(next,editor.getCodeHash())) throw new IllegalArgumentException("編集者とは異なるコードを指定してください");
        c.setCodeHash(encoder.encode(next)); credentials.save(c); state.advance(); states.save(state);
    }
}

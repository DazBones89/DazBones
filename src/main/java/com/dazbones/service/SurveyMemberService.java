package com.dazbones.service;

import com.dazbones.model.SurveyMember;
import com.dazbones.repository.SurveyMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurveyMemberService {

    private final SurveyMemberRepository repository;
    private final com.dazbones.repository.LoginCredentialRepository credentials;

    public SurveyMemberService(SurveyMemberRepository repository,com.dazbones.repository.LoginCredentialRepository credentials) {
        this.repository = repository;
        this.credentials=credentials;
    }

    public List<SurveyMember> getActiveMembers() {
        return repository.findByDeleteFlgOrderByNameAsc(0);
    }

    public List<SurveyMember> getAllMembers() {
        return repository.findAll();
    }

    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    public void addMember(String name) {
        if(name==null||name.isBlank()||name.trim().length()>100)throw new IllegalArgumentException("名前は1〜100文字で入力してください");
        SurveyMember member = new SurveyMember();
        member.setName(name.trim());
        member.setDeleteFlg(0);
        repository.save(member);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteMember(Long id) {
        SurveyMember member = repository.findById(id).orElse(null);
        if (member == null) {
            return;
        }

        member.setDeleteFlg(1);
        repository.save(member);
        credentials.findById("member-"+id).ifPresent(c->{
            c.setCodeHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(java.util.UUID.randomUUID().toString()));
            credentials.save(c);
        });
    }

    public void restore(Long id){var member=repository.findById(id).orElseThrow(()->new IllegalArgumentException("回答者が見つかりません"));member.setDeleteFlg(0);repository.save(member);}
}

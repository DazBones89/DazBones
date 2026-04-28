package com.dazbones.service;

import com.dazbones.model.SurveyMember;
import com.dazbones.repository.SurveyMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurveyMemberService {

    private final SurveyMemberRepository repository;

    public SurveyMemberService(SurveyMemberRepository repository) {
        this.repository = repository;
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
        SurveyMember member = new SurveyMember();
        member.setName(name.trim());
        member.setDeleteFlg(0);
        repository.save(member);
    }

    public void deleteMember(Long id) {
        SurveyMember member = repository.findById(id).orElse(null);
        if (member == null) {
            return;
        }

        member.setDeleteFlg(1);
        repository.save(member);
    }
}
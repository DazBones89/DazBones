package com.dazbones.repository;

import com.dazbones.model.SurveyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurveyMemberRepository extends JpaRepository<SurveyMember, Long> {

    List<SurveyMember> findByDeleteFlgOrderByNameAsc(Integer deleteFlg);

    Optional<SurveyMember> findByName(String name);

    boolean existsByName(String name);
}
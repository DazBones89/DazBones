package com.dazbones.repository;

import com.dazbones.model.SurveyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SurveyMemberRepository extends JpaRepository<SurveyMember, Long> {
    List<SurveyMember> findByDeleteFlg(Integer flg);
}
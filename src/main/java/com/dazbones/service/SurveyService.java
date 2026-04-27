package com.dazbones.service;

import com.dazbones.model.*;
import com.dazbones.repository.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class SurveyService {

    private final SurveyEventRepository eventRepo;
    private final SurveyAnswerRepository answerRepo;
    private final SurveyMemberRepository memberRepo;

    public SurveyService(SurveyEventRepository eventRepo,
                         SurveyAnswerRepository answerRepo,
                         SurveyMemberRepository memberRepo) {
        this.eventRepo = eventRepo;
        this.answerRepo = answerRepo;
        this.memberRepo = memberRepo;
    }

    // 土日判定
    private boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    public SurveyEvent getOrCreate(LocalDate date, boolean manual) {

        return eventRepo.findByTargetDate(date)
                .orElseGet(() -> {
                    SurveyEvent e = new SurveyEvent();
                    e.setTargetDate(date);
                    e.setManualFlg(manual);
                    return eventRepo.save(e);
                });
    }

    public void answer(LocalDate date, Long memberId, String status) {

        SurveyEvent e = getOrCreate(date, false);

        SurveyAnswer a = answerRepo
                .findBySurveyEventIdAndSurveyMemberId(e.getId(), memberId)
                .orElse(new SurveyAnswer());

        a.setSurveyEventId(e.getId());
        a.setSurveyMemberId(memberId);
        a.setAnswerStatus(status);

        answerRepo.save(a);
    }

    public Map<String, Object> getSummary(LocalDate date) {

        SurveyEvent e = eventRepo.findByTargetDate(date).orElse(null);

        Map<String, Object> res = new HashMap<>();

        boolean isWeekend = isWeekend(date);
        boolean hasManual = (e != null && e.getManualFlg());

        long total = memberRepo.findByDeleteFlg(0).size();

        if (e == null) {
            res.put("参加", 0);
            res.put("不参加", 0);
            res.put("未定", 0);
            res.put("未回答", total);
        } else {
            long join = answerRepo.countBySurveyEventIdAndAnswerStatus(e.getId(),"参加");
            long ng = answerRepo.countBySurveyEventIdAndAnswerStatus(e.getId(),"不参加");
            long pending = answerRepo.countBySurveyEventIdAndAnswerStatus(e.getId(),"未定");
            long answered = answerRepo.countBySurveyEventId(e.getId());

            res.put("参加", join);
            res.put("不参加", ng);
            res.put("未定", pending);
            res.put("未回答", total - answered);
        }

        // 色判定
        if (isWeekend && hasManual) res.put("type","mixed");
        else if (isWeekend) res.put("type","auto");
        else if (hasManual) res.put("type","manual");
        else res.put("type","none");

        return res;
    }
}
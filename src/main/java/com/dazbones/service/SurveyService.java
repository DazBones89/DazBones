package com.dazbones.service;

import com.dazbones.model.SurveyAnswer;
import com.dazbones.model.SurveyEvent;
import com.dazbones.model.SurveyMember;
import com.dazbones.repository.SurveyAnswerRepository;
import com.dazbones.repository.SurveyEventRepository;
import com.dazbones.repository.SurveyMemberRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class SurveyService {

    private final SurveyEventRepository eventRepository;
    private final SurveyAnswerRepository answerRepository;
    private final SurveyMemberRepository memberRepository;
    private final HolidayService holidayService;

    public SurveyService(SurveyEventRepository eventRepository,
                         SurveyAnswerRepository answerRepository,
                         SurveyMemberRepository memberRepository,
                         HolidayService holidayService) {
        this.eventRepository = eventRepository;
        this.answerRepository = answerRepository;
        this.memberRepository = memberRepository;
        this.holidayService = holidayService;
    }

    public List<SurveyMember> getActiveMembers() {
        return memberRepository.findByDeleteFlgOrderByNameAsc(0);
    }

    public SurveyEvent getOrCreateEvent(LocalDate date, boolean manualFlg) {
        return eventRepository.findByTargetDate(date)
                .orElseGet(() -> {
                    SurveyEvent event = new SurveyEvent();
                    event.setTargetDate(date);
                    event.setTitle("参加アンケート");
                    event.setManualFlg(manualFlg);
                    return eventRepository.save(event);
                });
    }

    public void createManualSurvey(LocalDate date, String title) {
        SurveyEvent event = getOrCreateEvent(date, true);
        event.setManualFlg(true);
        event.setTitle(title == null || title.isBlank() ? "追加アンケート" : title.trim());
        eventRepository.save(event);
    }

    public void saveAnswer(LocalDate date, Long memberId, String status, String comment) {
        SurveyEvent event = getOrCreateEvent(date, false);

        SurveyAnswer answer = answerRepository
                .findBySurveyEventIdAndSurveyMemberId(event.getId(), memberId)
                .orElse(new SurveyAnswer());

        answer.setSurveyEventId(event.getId());
        answer.setSurveyMemberId(memberId);
        answer.setAnswerStatus(status);
        answer.setComment(comment);

        answerRepository.save(answer);
    }

    public Map<String, Object> getSummary(LocalDate date) {
        SurveyEvent event = eventRepository.findByTargetDate(date).orElse(null);
        List<SurveyMember> members = memberRepository.findByDeleteFlgOrderByNameAsc(0);

        boolean autoTarget = isWeekend(date) || holidayService.isHoliday(date);
        boolean manual = event != null && Boolean.TRUE.equals(event.getManualFlg());

        long join = 0;
        long absent = 0;
        long undecided = 0;
        long answered = 0;

        if (event != null) {
            join = answerRepository.countBySurveyEventIdAndAnswerStatus(event.getId(), "参加");
            absent = answerRepository.countBySurveyEventIdAndAnswerStatus(event.getId(), "不参加");
            undecided = answerRepository.countBySurveyEventIdAndAnswerStatus(event.getId(), "未定");
            answered = answerRepository.countBySurveyEventId(event.getId());
        }

        long noAnswer = members.size() - answered;

        Map<String, Object> result = new HashMap<>();
        result.put("title", event != null ? event.getTitle() : (autoTarget ? "参加アンケート" : ""));
        result.put("参加", join);
        result.put("不参加", absent);
        result.put("未定", undecided);
        result.put("未回答", noAnswer);

        if (autoTarget && manual) {
            result.put("type", "mixed");
        } else if (autoTarget) {
            result.put("type", "auto");
        } else if (manual) {
            result.put("type", "manual");
        } else {
            result.put("type", "none");
        }

        return result;
    }

    public Map<String, Object> getDetail(LocalDate date, Long selectedMemberId) {
        SurveyEvent event = eventRepository.findByTargetDate(date).orElse(null);
        List<SurveyMember> members = memberRepository.findByDeleteFlgOrderByNameAsc(0);

        List<String> joinList = new ArrayList<>();
        List<String> absentList = new ArrayList<>();
        List<String> undecidedList = new ArrayList<>();
        List<String> noAnswerList = new ArrayList<>();

        String myStatus = "";
        String myComment = "";

        Map<Long, SurveyAnswer> answerMap = new HashMap<>();

        if (event != null) {
            List<SurveyAnswer> answers = answerRepository.findBySurveyEventId(event.getId());
            for (SurveyAnswer answer : answers) {
                answerMap.put(answer.getSurveyMemberId(), answer);
            }
        }

        for (SurveyMember member : members) {
            SurveyAnswer answer = answerMap.get(member.getId());

            if (answer == null) {
                noAnswerList.add(member.getName());
                continue;
            }

            if (selectedMemberId != null && selectedMemberId.equals(member.getId())) {
                myStatus = answer.getAnswerStatus();
                myComment = answer.getComment() == null ? "" : answer.getComment();
            }

            if ("参加".equals(answer.getAnswerStatus())) {
                joinList.add(member.getName());
            } else if ("不参加".equals(answer.getAnswerStatus())) {
                absentList.add(member.getName());
            } else if ("未定".equals(answer.getAnswerStatus())) {
                undecidedList.add(member.getName());
            } else {
                noAnswerList.add(member.getName());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", date.toString());
        result.put("title", event != null ? event.getTitle() : "参加アンケート");
        result.put("参加", joinList);
        result.put("不参加", absentList);
        result.put("未定", undecidedList);
        result.put("未回答", noAnswerList);
        result.put("myStatus", myStatus);
        result.put("myComment", myComment);

        return result;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
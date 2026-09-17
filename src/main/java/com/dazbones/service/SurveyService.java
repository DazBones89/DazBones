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
    private final com.dazbones.repository.SecurityStateRepository states;

    public SurveyService(SurveyEventRepository eventRepository,
                         SurveyAnswerRepository answerRepository,
                         SurveyMemberRepository memberRepository,
                         HolidayService holidayService, com.dazbones.repository.SecurityStateRepository states) {
        this.eventRepository = eventRepository;
        this.answerRepository = answerRepository;
        this.memberRepository = memberRepository;
        this.holidayService = holidayService; this.states = states;
    }

    public List<SurveyMember> getActiveMembers() {
        return memberRepository.findByDeleteFlgOrderByNameAsc(0);
    }

    @org.springframework.transaction.annotation.Transactional
    public SurveyEvent getOrCreateEvent(LocalDate date, boolean manualFlg) {
        states.lockState();
        return eventRepository.findByTargetDate(date)
                .orElseGet(() -> {
                    SurveyEvent event = new SurveyEvent();
                    event.setTargetDate(date);
                    event.setTitle("参加アンケート");
                    event.setManualFlg(manualFlg);
                    return eventRepository.save(event);
                });
    }

    @org.springframework.transaction.annotation.Transactional
    public void createManualSurvey(LocalDate date, String title) {
        states.lockState();
        if (eventRepository.findByTargetDate(date).filter(e -> Boolean.TRUE.equals(e.getManualFlg())).isPresent()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT);
        if(title != null && title.trim().length()>100) throw new IllegalArgumentException("タイトルは100文字以内です");
        SurveyEvent event = getOrCreateEvent(date, true);
        event.setManualFlg(true);
        event.setTitle(title == null || title.isBlank() ? "追加アンケート" : title.trim());
        eventRepository.save(event);
    }

    @org.springframework.transaction.annotation.Transactional
    public void saveAnswer(LocalDate date, Long memberId, String status, String comment) {
        saveAnswer(date, memberId, status, comment, null);
    }

    @org.springframework.transaction.annotation.Transactional
    public void saveAnswer(LocalDate date, Long memberId, String status, String comment, Long version) {
        states.lockState();
        SurveyMember member = memberRepository.findById(memberId).orElse(null);
        SurveyEvent existing = eventRepository.findByTargetDate(date).orElse(null);
        boolean target = isWeekend(date) || holidayService.isHoliday(date)
                || (existing != null && Boolean.TRUE.equals(existing.getManualFlg()));
        if (member == null || !Integer.valueOf(0).equals(member.getDeleteFlg()) || !target
                || !List.of("参加", "不参加", "未定").contains(status)
                || (comment != null && comment.length() > 255)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        SurveyEvent event = getOrCreateEvent(date, false);

        SurveyAnswer answer = answerRepository
                .findBySurveyEventIdAndSurveyMemberId(event.getId(), memberId)
                .orElse(new SurveyAnswer());

        if (version != null && !Objects.equals(version, answer.getId() == null ? -1L : answer.getVersion()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT);
        answer.setSurveyEventId(event.getId());
        answer.setSurveyMemberId(memberId);
        answer.setAnswerStatus(status);
        answer.setComment(comment);

        answerRepository.saveAndFlush(answer);
    }

    public Map<String, Object> getSummary(LocalDate date) {
        return getSummaries(date, date.plusDays(1)).get(date);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<LocalDate, Map<String, Object>> getSummaries(LocalDate start, LocalDate end) {
        var events = eventRepository.findByTargetDateGreaterThanEqualAndTargetDateLessThan(start, end);
        Set<Long> active = new HashSet<>();
        getActiveMembers().forEach(m -> active.add(m.getId()));
        Set<LocalDate> holidays = new HashSet<>();
        holidayService.getAll().forEach(h -> holidays.add(h.getHolidayDate()));
        Map<LocalDate, SurveyEvent> byDate = new HashMap<>();
        events.forEach(e -> byDate.put(e.getTargetDate(), e));
        Map<Long, Map<String, Long>> counts = new HashMap<>();
        if (!events.isEmpty()) for (var answer : answerRepository.findBySurveyEventIdIn(events.stream().map(SurveyEvent::getId).toList())) {
            if (active.contains(answer.getSurveyMemberId()) && List.of("参加", "不参加", "未定").contains(answer.getAnswerStatus()))
                counts.computeIfAbsent(answer.getSurveyEventId(), k -> new HashMap<>()).merge(answer.getAnswerStatus(), 1L, Long::sum);
        }
        Map<LocalDate, Map<String, Object>> result = new LinkedHashMap<>();
        for (LocalDate day = start; day.isBefore(end); day = day.plusDays(1)) {
            SurveyEvent event = byDate.get(day);
            boolean auto = isWeekend(day) || holidays.contains(day);
            boolean manual = event != null && Boolean.TRUE.equals(event.getManualFlg());
            Map<String, Long> count = event == null ? Map.of() : counts.getOrDefault(event.getId(), Map.of());
            Map<String, Object> summary = new HashMap<>();
            summary.put("title", event != null ? event.getTitle() : auto ? "参加アンケート" : "");
            long answered = 0;
            for (String status : List.of("参加", "不参加", "未定")) { long n = count.getOrDefault(status, 0L); summary.put(status, n); answered += n; }
            summary.put("未回答", Math.max(0L, active.size() - answered));
            summary.put("type", auto ? manual ? "mixed" : "auto" : manual ? "manual" : "none");
            result.put(day, summary);
        }
        return result;
    }

    public long countTodayNoAnswer() {
        Map<String, Object> summary = getSummary(LocalDate.now());
        String type = (String) summary.get("type");

        if ("none".equals(type)) {
            return 0;
        }

        Object value = summary.get("未回答");
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return 0;
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
        result.put("myVersion", answerMap.containsKey(selectedMemberId) ? answerMap.get(selectedMemberId).getVersion() : -1L);
        result.put("myStatus", myStatus);
        result.put("myComment", myComment);
        result.put("manual", event != null && Boolean.TRUE.equals(event.getManualFlg()));
        result.put("canAnswer", isWeekend(date) || holidayService.isHoliday(date) || (event != null && Boolean.TRUE.equals(event.getManualFlg())));

        return result;
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteManual(LocalDate date) {
        states.lockState();
        SurveyEvent event=eventRepository.findByTargetDate(date).orElseThrow(()->new IllegalArgumentException("アンケートが見つかりません"));
        if(!Boolean.TRUE.equals(event.getManualFlg())) throw new IllegalArgumentException("手動アンケートではありません");
        if(isWeekend(date)||holidayService.isHoliday(date)) {
            event.setManualFlg(false); event.setTitle("参加アンケート"); eventRepository.save(event);
        } else {
            answerRepository.deleteBySurveyEventId(event.getId());
            answerRepository.flush();eventRepository.delete(event);
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}

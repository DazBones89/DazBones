package com.dazbones.service;

import com.dazbones.model.*;
import com.dazbones.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class AttendanceService {
    @org.springframework.beans.factory.annotation.Autowired private AttendanceDateRepository extraDates;
    private final PlayerRepository players;
    private final SurveyMemberRepository members;
    private final HolidayRepository holidays;
    private final AttendanceAnswerRepository answers;
    private final SecurityStateRepository states;

    public AttendanceService(PlayerRepository players, SurveyMemberRepository members,
                             HolidayRepository holidays, AttendanceAnswerRepository answers, SecurityStateRepository states) {
        this.players = players; this.members = members; this.holidays = holidays; this.answers = answers; this.states = states;
    }

    public YearMonth month(String value) {
        try {
            YearMonth month = value == null || value.isBlank() ? YearMonth.now() : YearMonth.parse(value);
            if (month.getYear() < 1900 || month.getYear() > 2100) throw new IllegalArgumentException();
            return month;
        } catch (RuntimeException e) { throw new IllegalArgumentException("対象月は1900年〜2100年の年月を指定してください"); }
    }

    public List<LocalDate> dates(YearMonth month) {
        Set<LocalDate> holidayDates = new HashSet<>();
        holidays.findAllByOrderByHolidayDateAsc().forEach(h -> holidayDates.add(h.getHolidayDate()));
        extraDates.findAll().forEach(d -> holidayDates.add(d.date));
        return month.atDay(1).datesUntil(month.plusMonths(1).atDay(1))
                .filter(d -> d.getDayOfWeek().getValue() >= 6 || holidayDates.contains(d)).toList();
    }

    public Long ownPlayer(UserSession user) {
        if (user.getMemberId() == null) return null;
        return members.findById(user.getMemberId()).filter(m -> Integer.valueOf(0).equals(m.getDeleteFlg()))
                .map(SurveyMember::getPlayerId).orElse(null);
    }

    public List<AttendanceAnswer> answers(YearMonth month) {
        return answers.findByTargetDateBetweenOrderByTargetDateAsc(month.atDay(1), month.atEndOfMonth());
    }

    @Transactional
    public void bind(UserSession user, Long memberId, Long playerId) {
        if (!user.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        states.lockState();
        SurveyMember member = members.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("回答者を指定してください"));
        if (playerId != null) {
            if (!Integer.valueOf(0).equals(member.getDeleteFlg())) throw new IllegalArgumentException("削除済み回答者には紐付けできません");
            players.findById(playerId).filter(p -> Integer.valueOf(0).equals(p.getDeleteFlg()))
                    .orElseThrow(() -> new IllegalArgumentException("在籍中の選手を指定してください"));
            if (members.findAll().stream().anyMatch(m -> playerId.equals(m.getPlayerId()) && !memberId.equals(m.getId())))
                throw new IllegalArgumentException("この選手は別の回答者に紐付け済みです。先にその紐付けを解除してください");
        }
        member.setPlayerId(playerId);
        members.saveAndFlush(member);
    }

    @Transactional
    public AttendanceAnswer save(UserSession user, Long playerId, LocalDate date, String status, String memo, Long version) {
        // Binding changes and answer writes share a lock, so authorization cannot change midway through a write.
        states.lockState();
        if (user == null || !user.canManage())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        players.lockForFee(playerId).filter(p -> Integer.valueOf(0).equals(p.getDeleteFlg()))
                .orElseThrow(() -> new IllegalArgumentException("在籍中の選手を指定してください"));
        if (date == null || date.getYear() < 1900 || date.getYear() > 2100 || !dates(YearMonth.from(date)).contains(date))
            throw new IllegalArgumentException("出欠確認の対象日を指定してください");
        if (!Set.of("○", "△", "×").contains(status == null ? "" : status))
            throw new IllegalArgumentException("○・△・×を選択してください");
        if (memo != null && memo.length() > 500) throw new IllegalArgumentException("メモは500文字以内で入力してください");
        AttendanceAnswer answer = answers.findByPlayerIdAndTargetDate(playerId, date).orElse(null);
        Long expected = answer == null ? -1L : answer.getVersion();
        if (!Objects.equals(expected, version)) throw new ResponseStatusException(HttpStatus.CONFLICT,"他の画面で回答が更新されました。再読み込みして確認してください");
        if (answer == null) { answer = new AttendanceAnswer(); answer.setPlayerId(playerId); answer.setTargetDate(date); }
        answer.setStatus(status); answer.setMemo(memo == null ? "" : memo);
        return answers.saveAndFlush(answer);
    }
}

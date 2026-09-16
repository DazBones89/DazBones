package com.dazbones;

import com.dazbones.model.*;
import com.dazbones.repository.*;
import com.dazbones.service.FeeService;
import com.dazbones.service.SurveyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:site-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
        "app.auth.admin-code=test-admin-code", "app.auth.editor-code=test-editor-code"
})
@AutoConfigureMockMvc
class SiteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AttendanceAnswerRepository attendanceAnswers;
    @Autowired com.dazbones.service.AttendanceService attendanceService;
    @Autowired PlayerRepository players;
    @Autowired ScheduleRepository schedules;
    @Autowired NewsRepository news;
    @Autowired FeeRepository fees;
    @Autowired SurveyMemberRepository members;
    @Autowired SurveyEventRepository events;
    @Autowired SurveyAnswerRepository answers;
    @Autowired FeeService feeService;
    @Autowired SurveyService surveyService;
    @Autowired AnnualFeeRepository annualFees;
    @Autowired LoginCredentialRepository credentials;
    @Autowired SecurityStateRepository securityStates;
    @Autowired com.dazbones.service.CredentialService credentialService;
    @Autowired com.dazbones.service.SurveyMemberService memberService;
    @Autowired com.dazbones.service.HolidayService holidayService;
    @Autowired HolidayRepository holidays;
    @Autowired GearRepository gears;
    @Autowired com.dazbones.service.GearService gearService;

    @BeforeEach
    void cleanDatabase() {
        attendanceAnswers.deleteAll();
        annualFees.deleteAll(); gears.deleteAll(); holidays.deleteAll();
        credentials.deleteAll();securityStates.deleteAll();credentialService.initialize();
        answers.deleteAll();
        events.deleteAll();
        members.deleteAll();
        fees.deleteAll();
        players.deleteAll();
        schedules.deleteAll();
        news.deleteAll();
    }

    @Test
    void attendanceShowsOnlySortedWeekendsAndRegisteredHolidays() throws Exception {
        holidayService.addHoliday(LocalDate.of(2026, 9, 21), "祝日");
        assertThat(attendanceService.dates(java.time.YearMonth.of(2026, 9))).containsExactly(
                LocalDate.of(2026,9,5), LocalDate.of(2026,9,6), LocalDate.of(2026,9,12), LocalDate.of(2026,9,13),
                LocalDate.of(2026,9,19), LocalDate.of(2026,9,20), LocalDate.of(2026,9,21), LocalDate.of(2026,9,26), LocalDate.of(2026,9,27));
        var admin = login("admin");
        mvc.perform(get("/survey").session(admin)).andExpect(content().string(containsString("その他アンケート")))
                .andExpect(content().string(containsString("/survey/attendance")));
        mvc.perform(get("/survey/attendance").session(admin).param("month", "2026-09"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("在籍中の選手がいません")));
        mvc.perform(get("/survey/attendance")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/survey/attendance").session(admin).param("month", "bad")).andExpect(status().isBadRequest());
    }

    @Test
    void attendanceMemberCanOnlyAnswerLinkedPlayerAndAdminCanProxy() throws Exception {
        var a = player("出席選手A", 1, 1, "投手"); var b = player("出席選手B", 2, 1, "捕手");
        var m = member("本人"); m.setPlayerId(a.getId()); members.save(m);
        var memberSession = memberLogin(m.getId(), credentialService.issueMemberCode(m.getId()));
        mvc.perform(post("/survey/attendance/answer").session(memberSession).with(csrf())
                .param("playerId", a.getId().toString()).param("date", "2026-09-19").param("status", "○")
                .param("memo", "午前のみ").param("version", "-1")).andExpect(status().is3xxRedirection());
        assertThat(attendanceAnswers.findByPlayerIdAndTargetDate(a.getId(), LocalDate.of(2026,9,19)).orElseThrow().getMemo()).isEqualTo("午前のみ");
        mvc.perform(get("/survey/attendance").session(memberSession).param("month", "2026-09").param("playerId", b.getId().toString()))
                .andExpect(status().isOk()).andExpect(model().attribute("selectedPlayer", a.getId()));
        mvc.perform(post("/survey/attendance/answer").session(memberSession).with(csrf())
                .param("playerId", b.getId().toString()).param("date", "2026-09-19").param("status", "△").param("version", "-1"))
                .andExpect(status().isForbidden());
        var admin = login("admin");
        mvc.perform(post("/survey/attendance/answer").session(admin).with(csrf())
                .param("playerId", b.getId().toString()).param("date", "2026-09-19").param("status", "△").param("version", "-1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/survey/attendance").session(admin).param("month", "2026-09"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("午前のみ")))
                .andExpect(content().string(containsString("name=\"date\" value=\"2026-09-19\"")))
                .andExpect(content().string(containsString("id=\"day-2026-09-19\"")));
        assertThat(attendanceAnswers.count()).isEqualTo(2);
    }

    @Test
    void attendanceRejectsWeekdaysInvalidAnswersAndStaleUpdates() throws Exception {
        var p = player("検証選手", 1, 1, "投手");
        var session = login("admin"); var user = (UserSession)session.getAttribute("userSession");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> attendanceService.save(user,p.getId(),LocalDate.of(2026,9,16),"○","",-1L)).isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> attendanceService.save(user,p.getId(),LocalDate.of(2026,9,19),"×","",-1L)).isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> attendanceService.save(user,p.getId(),LocalDate.of(2026,9,19),"○","a".repeat(501),-1L)).isInstanceOf(IllegalArgumentException.class);
        attendanceService.save(user,p.getId(),LocalDate.of(2026,9,19),"○","",-1L);
        mvc.perform(post("/survey/attendance/answer").session(session).with(csrf())
                .param("playerId",p.getId().toString()).param("date","2026-09-19").param("status","△").param("memo","再入力").param("version","-1"))
                .andExpect(flash().attributeExists("errorMessage")).andExpect(flash().attribute("draftMemo","再入力"));
        assertThat(attendanceAnswers.findAll().get(0).getStatus()).isEqualTo("○");
        mvc.perform(post("/survey/attendance/answer").session(session)
                .param("playerId",p.getId().toString()).param("date","2026-09-19").param("status","○").param("version","0"))
                .andExpect(status().isForbidden());
        p.setDeleteFlg(1);players.save(p);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> attendanceService.save(user,p.getId(),LocalDate.of(2026,9,19),"○","",0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void attendanceBindingIsAdminOnlyAndUniqueAndCanBeClearedAfterDeletion() throws Exception {
        var p = player("紐付け選手",1,1,"投手"); var m = member("紐付け本人"); var other = member("別本人");
        var admin = login("admin");
        mvc.perform(post("/admin/attendance/bind").session(login("editor")).with(csrf())
                .param("memberId",m.getId().toString()).param("playerId",p.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/attendance/bind").session(admin).with(csrf())
                .param("memberId",m.getId().toString()).param("playerId",p.getId().toString())).andExpect(flash().attributeExists("successMessage"));
        mvc.perform(post("/admin/attendance/bind").session(admin).with(csrf())
                .param("memberId",other.getId().toString()).param("playerId",p.getId().toString())).andExpect(flash().attributeExists("errorMessage"));
        memberService.deleteMember(m.getId());
        mvc.perform(post("/admin/attendance/bind").session(admin).with(csrf()).param("memberId",m.getId().toString()))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(members.findById(m.getId()).orElseThrow().getPlayerId()).isNull();
    }

    private MockHttpSession login(String role) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                        .param("code", "test-" + role + "-code"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/main"))
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void directRoleUrlsCannotAuthenticate() throws Exception {
        for (String role : new String[]{"admin", "editor"}) {
            MockHttpSession session = new MockHttpSession();
            mvc.perform(get("/login/" + role).session(session)).andExpect(status().isNotFound());
            assertThat(session.getAttribute("userSession")).isNull();
            mvc.perform(get("/admin/news").session(session)).andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void loginRotatesSessionAndPersistsBothSecurityAndDisplayRole() throws Exception {
        MockHttpSession previous = new MockHttpSession();
        previous.setAttribute("selectedSurveyMemberId", 99L);
        MockHttpSession authenticated = (MockHttpSession) mvc.perform(post("/login").session(previous)
                        .with(csrf()).param("code", "test-admin-code"))
                .andExpect(redirectedUrl("/main")).andReturn().getRequest().getSession(false);
        assertThat(previous.isInvalid()).isTrue();
        assertThat(authenticated.getId()).isNotEqualTo(previous.getId());
        assertThat(authenticated.getAttribute("selectedSurveyMemberId")).isNull();
        assertThat(((UserSession) authenticated.getAttribute("userSession")).isAdmin()).isTrue();
        mvc.perform(get("/admin/news").session(authenticated)).andExpect(status().isOk())
                .andExpect(content().string(containsString("お知らせ管理")));
    }

    @Test
    void incorrectCodeDoesNotAuthenticate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/login").session(session).with(csrf()).param("code", "wrong"))
                .andExpect(redirectedUrl("/login"));
        assertThat(session.getAttribute("userSession")).isNull();
    }

    @Test
    void csrfIsRequiredForLoginAndUpdates() throws Exception {
        mvc.perform(post("/login").param("code", "test-admin-code")).andExpect(status().isForbidden());
        MockHttpSession session = login("admin");
        mvc.perform(post("/admin/schedules").session(session)
                        .param("title", "練習").param("eventDate", "2026-09-12"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/schedules").session(session).with(csrf().useInvalidToken())
                        .param("title", "練習").param("eventDate", "2026-09-12"))
                .andExpect(status().isForbidden());
        assertThat(schedules.count()).isZero();
    }

    @Test
    void editorCannotUseAdminOperations() throws Exception {
        MockHttpSession session = login("editor");
        mvc.perform(get("/admin/news").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/gear/delete").session(session).with(csrf()).param("id", "1"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/schedules").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/survey-members").session(session)).andExpect(status().isOk());
    }

    @Test
    void anonymousApiIsUnauthorized() throws Exception {
        mvc.perform(get("/api/survey/events").param("start", "2026-09-01").param("end", "2026-10-01"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void logoutRequiresPostAndInvalidatesSession() throws Exception {
        MockHttpSession session = login("admin");
        mvc.perform(get("/logout").session(session));
        assertThat(session.isInvalid()).isFalse();
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void loginPageContainsUsableCsrfField() throws Exception {
        var page = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var matcher = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                .matcher(page.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        mvc.perform(post("/login").session((MockHttpSession) page.getRequest().getSession())
                        .param("_csrf", matcher.group(1)).param("code", "test-editor-code"))
                .andExpect(redirectedUrl("/main"));
    }

    @Test
    void renderedCalendarsIncludeAssetsAndInitialization() throws Exception {
        for (String path : new String[]{"/schedule", "/survey"}) {
            mvc.perform(get(path).session(login("editor"))).andExpect(status().isOk())
                    .andExpect(content().string(containsString("index.global.min.js")))
                    .andExpect(content().string(containsString("new FullCalendar.Calendar")));
        }
        mvc.perform(get("/photo")).andExpect(status().isOk()).andExpect(view().name("photo"));
    }

    @Test
    void scheduleCanBeCreatedEditedListedAndDeleted() throws Exception {
        MockHttpSession session = login("editor");
        mvc.perform(post("/admin/schedules").session(session).with(csrf()).param("title", "練習")
                        .param("eventDate", "2026-09-12").param("startTime", "10:00").param("endTime", "12:00"))
                .andExpect(redirectedUrl("/admin/schedules"));
        Long id = schedules.findAll().get(0).getId();
        mvc.perform(get("/admin/schedules/{id}/edit", id).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("練習")));
        mvc.perform(post("/admin/schedules/{id}/edit", id).session(session).with(csrf())
                        .param("title", "練習試合").param("eventDate", "2026-09-12")
                        .param("location", "球場").param("resultStatus", "勝利").param("score", "5-3"))
                .andExpect(redirectedUrl("/admin/schedules"));
        mvc.perform(get("/api/schedules").param("start", "2026-09-01T00:00:00+09:00")
                        .param("end", "2026-10-01T00:00:00+09:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("練習試合"))
                .andExpect(jsonPath("$[0].allDay").value(true))
                .andExpect(jsonPath("$[0].extendedProps.resultStatus").value("勝利"));
        mvc.perform(post("/admin/schedules/{id}/delete", id).session(session).with(csrf()))
                .andExpect(redirectedUrl("/admin/schedules"));
        assertThat(schedules.count()).isZero();
    }

    @Test
    void invalidScheduleUpdatePreservesDataAndEditTarget() throws Exception {
        Schedule s = new Schedule();
        s.setTitle("元の予定");
        s.setEventDate(LocalDate.of(2026, 9, 12));
        s = schedules.save(s);
        mvc.perform(post("/admin/schedules/{id}/edit", s.getId()).session(login("editor")).with(csrf())
                        .param("title", "変更").param("eventDate", "2026-09-12")
                        .param("startTime", "12:00").param("endTime", "10:00"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("scheduleForm"))
                .andExpect(content().string(containsString("終了時間は開始時間より後")))
                .andExpect(content().string(containsString("/admin/schedules/" + s.getId() + "/edit")));
        assertThat(schedules.findById(s.getId()).orElseThrow().getTitle()).isEqualTo("元の予定");
    }

    @Test
    void missingScheduleIs404() throws Exception {
        mvc.perform(get("/admin/schedules/999999/edit").session(login("editor")))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCalendarRangesReturnJson400() throws Exception {
        for (String end : new String[]{"invalid", "2026-08-01", "2027-09-01"}) {
            mvc.perform(get("/api/schedules").param("start", "2026-09-01").param("end", end))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(get("/api/schedules").param("start", "2026-09-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void surveyAcceptsOffsetDateRanges() throws Exception {
        mvc.perform(get("/api/survey/day-types").session(login("editor"))
                        .param("start", "2026-09-12T00:00:00+09:00").param("end", "2026-09-14T00:00:00+09:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$['2026-09-12']").value("auto"));
    }

    @Test
    void futureNewsCannotBeReadByDirectUrl() throws Exception {
        News n = new News();
        n.setTitle("公開前の記事");
        n.setContent("まだ公開しない本文");
        n.setPublishedAt(LocalDateTime.now().plusDays(10));
        n = news.save(n);
        mvc.perform(get("/news/{id}", n.getId())).andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("まだ公開しない本文"))));
        n.setPublishedAt(LocalDateTime.now().minusDays(1));
        news.save(n);
        mvc.perform(get("/news/{id}", n.getId())).andExpect(status().isOk());
    }

    private SurveyMember member(String name) {
        SurveyMember m = new SurveyMember();m.setName(name);return members.save(m);
    }

    private MockHttpSession memberLogin(Long id, String code) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(csrf()).param("loginId","member-"+id).param("code",code))
                .andExpect(redirectedUrl("/main")).andReturn().getRequest().getSession(false);
    }

    @Test
    void memberMayAnswerOnlyForSelfAndCannotCreateOrDeleteSurvey() throws Exception {
        SurveyMember alice=member("本人"),bob=member("別人");
        MockHttpSession session=memberLogin(alice.getId(),credentialService.issueMemberCode(alice.getId()));
        mvc.perform(get("/survey").session(session)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"manualSurveyTitle\""))));
        mvc.perform(post("/survey/answer").session(session).with(csrf()).param("date","2026-09-12")
                .param("memberId",alice.getId().toString()).param("status","参加")).andExpect(status().isOk());
        mvc.perform(post("/survey/answer").session(session).with(csrf()).param("date","2026-09-12")
                .param("memberId",bob.getId().toString()).param("status","参加")).andExpect(status().isForbidden());
        mvc.perform(get("/api/survey/detail").session(session).param("date","2026-09-12").param("memberId",bob.getId().toString()))
                .andExpect(status().isForbidden());
        for(String path:new String[]{"/survey/manual","/survey/manual/delete"})
            mvc.perform(post(path).session(session).with(csrf()).param("date","2026-09-12")).andExpect(status().isForbidden());
        assertThat(answers.count()).isEqualTo(1);
    }

    @Test
    void sharedEditorCannotImpersonateOrCreateSurvey() throws Exception {
        SurveyMember m=member("回答者");MockHttpSession session=login("editor");
        mvc.perform(post("/survey/answer").session(session).with(csrf()).param("date","2026-09-12")
                .param("memberId",m.getId().toString()).param("status","参加")).andExpect(status().isForbidden());
        mvc.perform(post("/survey/manual").session(session).with(csrf()).param("date","2026-09-14")).andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateProxyAnswerAndDeleteWeekdaySurvey() throws Exception {
        SurveyMember m=member("代理回答先");MockHttpSession session=login("admin");
        mvc.perform(post("/survey/manual").session(session).with(csrf()).param("date","2026-09-14").param("title","臨時活動"))
                .andExpect(status().isOk());
        mvc.perform(post("/survey/answer").session(session).with(csrf()).param("date","2026-09-14")
                .param("memberId",m.getId().toString()).param("status","参加")).andExpect(status().isOk());
        mvc.perform(post("/survey/manual/delete").session(session).with(csrf()).param("date","2026-09-14")).andExpect(status().isOk());
        assertThat(answers.count()).isZero();assertThat(events.count()).isZero();
    }

    @Test
    void deletingManualWeekendPreservesAutomaticAnswers() {
        var m=member("回答者");var date=LocalDate.of(2026,9,12);
        surveyService.createManualSurvey(date,"追加");surveyService.saveAnswer(date,m.getId(),"参加","");
        surveyService.deleteManual(date);
        assertThat(answers.count()).isEqualTo(1);assertThat(surveyService.getSummary(date)).containsEntry("type","auto");
    }

    @Test
    void deletingAndRestoringMemberRequiresFreshCode() throws Exception {
        var m=member("復元対象");String code=credentialService.issueMemberCode(m.getId());
        var oldSession=memberLogin(m.getId(),code);
        memberService.deleteMember(m.getId());memberService.restore(m.getId());
        String newCode=credentialService.issueMemberCode(m.getId());
        mvc.perform(get("/api/survey/day-types").session(oldSession).param("start","2026-09-12").param("end","2026-09-13"))
                .andExpect(status().isUnauthorized());
        assertThat(credentialService.authenticate("member-"+m.getId(),code)).isNull();
        assertThat(credentialService.authenticate("member-"+m.getId(),newCode)).isNotNull();
    }

    @Test
    void codeIssuanceIsAdminOnlyAndRendersOneTimeCode() throws Exception {
        var m=member("コード発行先");
        mvc.perform(post("/admin/survey-members/{id}/code",m.getId()).session(login("editor")).with(csrf())).andExpect(status().isForbidden());
        var response=mvc.perform(post("/admin/survey-members/{id}/code",m.getId()).session(login("admin")).with(csrf()))
                .andExpect(redirectedUrl("/admin/survey-members")).andReturn();
        assertThat(response.getFlashMap().get("issuedCode")).isNotNull();
        mvc.perform(get("/admin/survey-members").session(login("admin")).flashAttrs(response.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("コード発行")));
    }

    @Test
    void adminCodeChangeRevokesAllExistingSessionsAndPersistsNewCode() throws Exception {
        var admin=login("admin");var editor=login("editor");var otherAdmin=login("admin");
        var m=member("ログイン中");var memberSession=memberLogin(m.getId(),credentialService.issueMemberCode(m.getId()));
        mvc.perform(post("/admin/code").session(admin).with(csrf()).param("currentCode","test-admin-code")
                .param("newCode","replacement-code").param("confirmation","replacement-code"))
                .andExpect(redirectedUrl("/login"));
        for(var old:new MockHttpSession[]{editor,otherAdmin,memberSession})
            mvc.perform(get("/api/survey/day-types").session(old).param("start","2026-09-12").param("end","2026-09-13"))
                    .andExpect(status().isUnauthorized());
        credentialService.initialize(); // restart bootstrap must not overwrite changed credentials
        assertThat(credentialService.authenticate(null,"test-admin-code")).isNull();
        assertThat(credentialService.authenticate(null,"replacement-code").isAdmin()).isTrue();
    }

    @Test
    void incorrectCurrentCodeDoesNotRevokeSessions() throws Exception {
        var admin=login("admin");
        mvc.perform(post("/admin/code").session(admin).with(csrf()).param("currentCode","wrong")
                .param("newCode","replacement-code").param("confirmation","replacement-code"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("現在のコードが違います")));
        mvc.perform(get("/admin/news").session(admin)).andExpect(status().isOk());
    }

    @Test
    void memberNewsNeverAppearsInPublicListOrDetail() throws Exception {
        News n=new News();n.setTitle("限定情報");n.setContent("内部向け本文");n.setAudience("MEMBERS");n.setPublishedAt(LocalDateTime.now().minusDays(1));n=news.save(n);
        mvc.perform(get("/news")).andExpect(status().isOk()).andExpect(content().string(not(containsString("限定情報"))));
        mvc.perform(get("/news/{id}",n.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/news/members")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/news/members").session(login("editor"))).andExpect(status().isOk()).andExpect(content().string(containsString("限定情報")));
        mvc.perform(get("/news/{id}",n.getId()).session(login("editor"))).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(not(containsString("限定情報"))));
    }

    @Test
    void newsFormHasOneAudienceSelectorAndSavesAudience() throws Exception {
        var session=login("admin");
        String html=mvc.perform(get("/admin/news/new").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(java.util.regex.Pattern.compile("name=\"audience\"").matcher(html).results().count()).isEqualTo(1);
        mvc.perform(post("/admin/news").session(session).with(csrf()).param("title","限定")
                .param("content","本文").param("publishedAt","2026-09-01T00:00").param("audience","MEMBERS"))
                .andExpect(redirectedUrl("/admin/news"));
        assertThat(news.findAll().get(0).getAudience()).isEqualTo("MEMBERS");
    }

    @Test
    void annualFeesKeepYearsAndDisplayPersonalAndTeamTotals() throws Exception {
        var p=player("年会費選手",1,1,"投手");var session=login("admin");
        for(int year:new int[]{2025,2026}) {
            mvc.perform(post("/fee/update").session(session).with(csrf()).param("playerId",p.getId().toString())
                    .param("fiscalYear",String.valueOf(year)).param("amount","10000").param("paidAmount",year==2025?"10000":"4000"))
                    .andExpect(redirectedUrl("/fee?year="+year));
        }
        assertThat(annualFees.count()).isEqualTo(2);
        mvc.perform(get("/fee").session(session).param("year","2026")).andExpect(status().isOk())
                .andExpect(content().string(containsString("20,000円"))).andExpect(content().string(containsString("14,000円")))
                .andExpect(content().string(containsString("6,000円")));
        mvc.perform(get("/fee/player/{id}",p.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("2025年度"))).andExpect(content().string(containsString("2026年度")));
    }

    @Test
    void annualFeeRejectsOverpaymentAndStaleUpdate() throws Exception {
        var p=player("請求先",1,1,"投手");var session=login("admin");
        mvc.perform(post("/fee/update").session(session).with(csrf()).param("playerId",p.getId().toString())
                .param("fiscalYear","2026").param("amount","100").param("paidAmount","200"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("入金額は請求額以下")));
        assertThat(annualFees.count()).isZero();
        mvc.perform(post("/fee/update").session(session).with(csrf()).param("playerId",p.getId().toString())
                .param("fiscalYear","2026").param("amount","100").param("paidAmount","0"));
        mvc.perform(post("/fee/update").session(session).with(csrf()).param("playerId",p.getId().toString())
                .param("fiscalYear","2026").param("amount","200").param("paidAmount","0"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("別の操作で更新")));
        assertThat(annualFees.findAll().get(0).getAmount()).isEqualTo(100);
    }

    @Test
    void yearlyActivityHistoryExcludesOtherYearsAndFutureDates() throws Exception {
        for(int year:new int[]{2024,2025,2099}){var s=new Schedule();s.setTitle("記録"+year);s.setEventDate(LocalDate.of(year,1,1));s.setResultStatus("勝利");schedules.save(s);}
        mvc.perform(get("/history").param("year","2025")).andExpect(status().isOk())
                .andExpect(content().string(containsString("記録2025"))).andExpect(content().string(not(containsString("記録2024"))))
                .andExpect(content().string(not(containsString("記録2099"))));
    }

    @Test
    void invalidCsvDoesNotSaveEarlierValidRows() throws Exception {
        var csv=new org.springframework.mock.web.MockMultipartFile("file","holidays.csv","text/csv","2026-01-01,元日\ninvalid,不正".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        org.assertj.core.api.Assertions.assertThatThrownBy(()->holidayService.importCsv(csv)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("2行目");
        assertThat(holidays.count()).isZero();
        var valid=new org.springframework.mock.web.MockMultipartFile("file","holidays.csv","text/csv","\uFEFFdate,name\n2026-01-01,元日".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(holidayService.importCsv(valid)).isEqualTo(1);
    }

    @Test
    void gearUpdateKeepsCreationDateAndDeletedOwner() throws Exception {
        var p=player("所有者",1,1,"投手");Gear g=new Gear();g.setName("バット");g.setOwnerId(p.getId());g=gears.save(g);
        var original=gears.findById(g.getId()).orElseThrow().getCreatedAt();p.setDeleteFlg(1);players.save(p);
        mvc.perform(post("/gear/save").session(login("editor")).with(csrf()).param("id",g.getId().toString())
                .param("name","更新バット").param("ownerId",p.getId().toString())).andExpect(redirectedUrl("/gear"));
        var updated=gears.findById(g.getId()).orElseThrow();assertThat(updated.getCreatedAt()).isEqualTo(original);assertThat(updated.getOwnerId()).isEqualTo(p.getId());
        mvc.perform(get("/gear").session(login("editor"))).andExpect(status().isOk()).andExpect(content().string(containsString("所有者（退部）")));
    }

    @Test
    void invalidPlayerStatsAndFakeImageAreNotSaved() throws Exception {
        var session=login("editor");
        mvc.perform(post("/players/add").session(session).with(csrf()).param("name","不正成績").param("atBats","1").param("hits","2"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("ヒット数は打数以下")));
        var fake=new org.springframework.mock.web.MockMultipartFile("imageFile","fake.jpg","image/jpeg","not an image".getBytes());
        mvc.perform(multipart("/players/add").file(fake).session(session).with(csrf()).param("name","偽画像"))
                .andExpect(status().isOk()).andExpect(view().name("playerAdd"));
        assertThat(players.count()).isZero();
    }

    private Player player(String name, int number, int hits, String position) {
        Player p = new Player();
        p.setName(name);
        p.setBackNumber(number);
        p.setDeleteFlg(0);
        p.setAtBats(10);
        p.setHits(hits);
        PlayerPosition pos = new PlayerPosition();
        pos.setPosition(position);
        pos.setPlayer(p);
        p.getPositions().add(pos);
        return players.save(p);
    }

    @Test
    void playersAndPositionsRenderOutsidePersistenceContext() throws Exception {
        Player p = player("確認用選手", 10, 10, "投手");
        mvc.perform(get("/players")).andExpect(status().isOk())
                .andExpect(content().string(containsString("確認用選手")))
                .andExpect(content().string(containsString("投手")))
                .andExpect(content().string(containsString("1.000")));
        mvc.perform(get("/players/{id}/edit", p.getId()).session(login("editor")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("確認用選手")));
    }

    @Test
    void playerPositionsCanBeUpdated() throws Exception {
        Player p = player("元の名前", 10, 2, "投手");
        mvc.perform(post("/players/{id}/edit", p.getId()).session(login("editor")).with(csrf())
                        .param("name", "新しい名前").param("positions", "捕手").param("atBats", "10").param("hits", "3"))
                .andExpect(redirectedUrl("/players"));
        Player updated = players.findById(p.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("新しい名前");
        assertThat(updated.getPositions()).extracting(PlayerPosition::getPosition).containsExactly("捕手");
    }

    @Test
    void requestedPlayerSortChangesOrder() throws Exception {
        Player first = player("背番号先", 1, 1, "外野手");
        Player second = player("打率先", 99, 9, "投手");
        var result = mvc.perform(get("/players").param("sort", "average"))
                .andExpect(status().isOk()).andReturn();
        @SuppressWarnings("unchecked")
        var sorted = (java.util.List<Player>) result.getModelAndView().getModel().get("players");
        assertThat(sorted).extracting(Player::getId).containsExactly(second.getId(), first.getId());
    }

    @Test
    void unpaidCountExcludesDeletedPlayersAndDoesNotCreateRows() {
        player("在籍", 1, 1, "投手");
        Player deleted = player("退部", 2, 1, "捕手");
        deleted.setDeleteFlg(1);
        players.save(deleted);
        Fee fee = new Fee();
        fee.setPlayerId(deleted.getId());
        fees.save(fee);
        assertThat(feeService.countUnpaid()).isEqualTo(1);
        assertThat(fees.count()).isEqualTo(1);
    }

    @Test
    void surveyCountsExcludeAnswersFromDeletedMembers() {
        SurveyMember member = new SurveyMember();
        member.setName("元回答者");
        member = members.save(member);
        LocalDate day = LocalDate.of(2026, 9, 12);
        surveyService.saveAnswer(day, member.getId(), "参加", "");
        member.setDeleteFlg(1);
        members.save(member);
        assertThat(surveyService.getSummary(day)).containsEntry("参加", 0L).containsEntry("未回答", 0L);
    }
}

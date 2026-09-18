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
        "app.auth.master-code=test-admin-code", "app.auth.player-code=test-editor-code"
})
@AutoConfigureMockMvc
class SiteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired com.dazbones.service.InputService inputService;
    @Autowired com.dazbones.service.PlayerVisibility visibility;
    @Autowired SiteSettingRepository settings;
    @Autowired AttendanceDateRepository extraDates;
    @Autowired AuditEntryRepository audits;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
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
        settings.deleteAll(); extraDates.deleteAll();
        audits.deleteAll();
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
    void staleScheduleUpdatesAndDeletesAreRejected() throws Exception {
        var session=login("editor");
        mvc.perform(post("/admin/schedules").session(session).with(csrf()).param("title","初回").param("eventDate","2026-09-19"))
                .andExpect(status().is3xxRedirection());
        var original=schedules.findAll().get(0);
        mvc.perform(post("/admin/schedules/{id}/edit",original.getId()).session(session).with(csrf())
                .param("title","先の編集").param("eventDate","2026-09-19").param("version","0"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/schedules/{id}/edit",original.getId()).session(session).with(csrf())
                .param("title","古い編集").param("eventDate","2026-09-19").param("version","0"))
                .andExpect(view().name("admin/scheduleForm")).andExpect(model().attributeHasErrors("scheduleForm"));
        mvc.perform(post("/admin/schedules/{id}/delete",original.getId()).session(session).with(csrf()).param("version","0"))
                .andExpect(status().isConflict());
        assertThat(schedules.findById(original.getId()).orElseThrow().getTitle()).isEqualTo("先の編集");
    }

    @Test
    void concurrentFirstSurveyAnswersCannotOverwriteEachOther() throws Exception {
        var m=member("並行回答"); var day=LocalDate.of(2026,9,19);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var gate=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> write=()->{
            gate.await();
            try { surveyService.saveAnswer(day,m.getId(),"参加","",-1L);return true; }
            catch(org.springframework.web.server.ResponseStatusException e){assertThat(e.getStatusCode().value()).isEqualTo(409);return false;}
        };
        try {
            var a=pool.submit(write);var b=pool.submit(write);gate.countDown();
            assertThat(java.util.List.of(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        } finally {pool.shutdownNow();}
        assertThat(events.count()).isEqualTo(1); assertThat(answers.count()).isEqualTo(1);
    }

    @Test
    void surveyRangeUsesBoundedQueriesAndExcludesDeletedMembers() {
        var active=member("有効");var deleted=member("削除対象");var day=LocalDate.of(2026,9,19);
        surveyService.saveAnswer(day,active.getId(),"参加","");surveyService.saveAnswer(day,deleted.getId(),"不参加","");
        memberService.deleteMember(deleted.getId());
        var stats=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);stats.clear();
        try {
            var summaries=surveyService.getSummaries(day.withDayOfMonth(1),day.plusMonths(2).withDayOfMonth(1));
            assertThat(summaries.get(day).get("参加")).isEqualTo(1L);
            assertThat(summaries.get(day).get("不参加")).isEqualTo(0L);
            assertThat(summaries.get(day).get("未回答")).isEqualTo(0L);
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(4L);
        } finally {stats.setStatisticsEnabled(false);}
    }

    @Test
    void auditRecordsSafeMetadataAndIsRestrictedToAdmin() throws Exception {
        var session=login("editor");
        mvc.perform(post("/admin/schedules").session(session).with(csrf()).param("title","非公開の本文").param("eventDate","2026-09-19"))
                .andExpect(status().is3xxRedirection());
        var audit=audits.findAll().get(0);
        assertThat(audit.getLoginId()).isEqualTo("player");assertThat(audit.getOutcome()).isEqualTo("完了");
        assertThat(audit.getTarget()).isEqualTo("/admin/schedules");
        mvc.perform(get("/admin/audit").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/audit").session(login("admin"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("player"))).andExpect(content().string(not(containsString("非公開の本文"))));
    }

    @Test
    void productionAssetsAreLocalAndReadinessWorks() throws Exception {
        mvc.perform(get("/schedule")).andExpect(content().string(containsString("/css/site.css")))
                .andExpect(content().string(not(containsString("cdn.tailwindcss.com"))));
        mvc.perform(get("/css/site.css")).andExpect(status().isOk());
        mvc.perform(get("/vendor/fullcalendar-6.1.10.min.js")).andExpect(status().isOk());
        mvc.perform(get("/health/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
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
        for (String path : new String[]{"/schedule"}) {
            mvc.perform(get(path).session(login("editor"))).andExpect(status().isOk())
                    .andExpect(content().string(containsString("/vendor/fullcalendar-6.1.10.min.js")))
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
        mvc.perform(post("/admin/schedules/{id}/edit", id).session(session).with(csrf()).param("version", schedules.findById(id).orElseThrow().getVersion().toString())
                        .param("title", "練習試合").param("eventDate", "2026-09-12")
                        .param("location", "球場").param("resultStatus", "勝利").param("score", "5-3"))
                .andExpect(redirectedUrl("/admin/schedules"));
        mvc.perform(get("/api/schedules").param("start", "2026-09-01T00:00:00+09:00")
                        .param("end", "2026-10-01T00:00:00+09:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("練習試合"))
                .andExpect(jsonPath("$[0].allDay").value(true))
                .andExpect(jsonPath("$[0].extendedProps.resultStatus").value("勝利"));
        mvc.perform(post("/admin/schedules/{id}/delete", id).session(session).with(csrf()).param("version", schedules.findById(id).orElseThrow().getVersion().toString()))
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
    void sharedEditorCannotImpersonateOrCreateSurvey() throws Exception {
        SurveyMember m=member("回答者");MockHttpSession session=login("editor");
        mvc.perform(post("/survey/answer").session(session).with(csrf()).param("date","2026-09-12")
                .param("memberId",m.getId().toString()).param("status","参加")).andExpect(status().isForbidden());
        mvc.perform(post("/survey/manual").session(session).with(csrf()).param("date","2026-09-14")).andExpect(status().isForbidden());
    }



    @Test
    void deletingManualWeekendPreservesAutomaticAnswers() {
        var m=member("回答者");var date=LocalDate.of(2026,9,12);
        surveyService.createManualSurvey(date,"追加");surveyService.saveAnswer(date,m.getId(),"参加","");
        surveyService.deleteManual(date);
        assertThat(answers.count()).isEqualTo(1);assertThat(surveyService.getSummary(date)).containsEntry("type","auto");
    }







    @Test
    void incorrectCurrentCodeDoesNotRevokeSessions() throws Exception {
        var admin=login("admin");
        mvc.perform(post("/admin/code").session(admin).with(csrf()).param("currentCode","wrong")
                .param("newCode","replacement-code").param("confirmation","replacement-code"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("現在のmasterコードが違います")));
        mvc.perform(get("/admin/news").session(admin)).andExpect(status().isOk());
    }

    @Test
    void memberNewsNeverAppearsInPublicListOrDetail() throws Exception {
        News n=new News();n.setTitle("限定情報");n.setContent("内部向け本文");n.setAudience("MEMBERS");n.setPublishedAt(LocalDateTime.now().minusDays(1));n=news.save(n);
        mvc.perform(get("/news")).andExpect(status().isOk()).andExpect(content().string(not(containsString("限定情報"))));
        mvc.perform(get("/news/{id}",n.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/news/members")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/news").session(login("editor"))).andExpect(status().isOk()).andExpect(content().string(containsString("限定情報")));
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
        mvc.perform(post("/players/{id}/edit", p.getId()).session(login("editor")).with(csrf()).param("version",p.getVersion().toString())
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

    @Test void sharedRolesAndRetiredLogins() throws Exception {
        var session=login("editor");
        for(String path:new String[]{"/admin/news","/admin/schedules","/admin/upload/group-photo","/players/stats","/fee","/gear"})
            mvc.perform(get(path).session(session)).andExpect(status().isOk());
        for(String path:new String[]{"/admin/holidays","/admin/code","/admin/audit","/admin/player-settings","/admin/survey-members"})
            mvc.perform(get(path).session(session)).andExpect(status().isForbidden());
        var m=member("旧本人");String old=credentialService.issueMemberCode(m.getId());
        assertThat(credentialService.authenticate("member-"+m.getId(),old)).isNull();
        mvc.perform(get("/survey").session(session)).andExpect(redirectedUrl("/survey/attendance"));
        mvc.perform(get("/survey/attendance").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("data-page=\"attendance\"")))
            .andExpect(content().string(not(containsString("一括入力"))));
        mvc.perform(get("/input").session(session).param("tab","fee").param("year","2025")).andExpect(redirectedUrl("/fee?year=2025"));
        mvc.perform(get("/players/stats")).andExpect(redirectedUrl("/login"));
    }
    @Test void codeChangeRevokesBothRolesAndPersists() throws Exception {
        var master=login("admin");var player=login("editor");var other=login("admin");
        mvc.perform(post("/admin/code").session(master).with(csrf()).param("role","player").param("currentCode","test-admin-code").param("newCode","new-shared").param("confirmation","new-shared")).andExpect(redirectedUrl("/login"));
        for(var session:new MockHttpSession[]{player,other})mvc.perform(get("/api/input").session(session).param("year","2026").param("month","2026-09")).andExpect(status().isUnauthorized());
        credentialService.initialize();assertThat(credentialService.authenticate(null,"test-editor-code")).isNull();assertThat(credentialService.authenticate(null,"new-shared").getRole()).isEqualTo("player");
    }
    @Test void attendanceAllowsSharedNamesCrossAndManualWeekdays() throws Exception {
        var a=player("選手A",1,1,"投手");var b=player("選手B",2,1,"捕手");var session=login("editor");
        mvc.perform(post("/api/input/date").session(session).with(csrf()).param("date","2026-09-16")).andExpect(status().isOk());
        for(var p:java.util.List.of(a,b))mvc.perform(post("/api/input/attendance").session(session).with(csrf()).param("playerId",p.getId().toString()).param("date","2026-09-16").param("status","×").param("memo","欠席").param("version","-1")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
        assertThat(attendanceAnswers.count()).isEqualTo(2);
        mvc.perform(get("/api/input").session(session).param("year","2026").param("month","2026-09")).andExpect(status().isOk()).andExpect(jsonPath("$.dates",org.hamcrest.Matchers.hasItem("2026-09-16"))).andExpect(jsonPath("$.answers.length()").value(2));
        mvc.perform(post("/api/input/attendance").session(session).with(csrf()).param("playerId",a.getId().toString()).param("date","2026-09-16").param("status","○").param("version","-1")).andExpect(status().isConflict());
    }
    @Test void feesPreserveYearCommentsAndRejectStaleSaves() throws Exception {
        var p=player("部費選手",1,1,"投手");var u=(UserSession)login("editor").getAttribute("userSession");
        inputService.fee(u,p.getId(),2025,true,"5000円",-1L);inputService.fee(u,p.getId(),2026,false,"",-1L);
        assertThat(annualFees.findByPlayerIdAndFiscalYear(p.getId(),2025).orElseThrow().isPaid()).isTrue();
        assertThat(annualFees.findByPlayerIdAndFiscalYear(p.getId(),2026).orElseThrow().isPaid()).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(()->inputService.fee(u,p.getId(),2026,true,"",-1L)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(annualFees.findByPlayerIdAndFiscalYear(p.getId(),2025).orElseThrow().getComment()).isEqualTo("5000円");
    }
    @Test void hiddenFieldsAreNotRenderedOrOverwritten() throws Exception {
        var p=player("非公開氏名",1,3,"投手");visibility.set("name",false);visibility.set("hits",false);var session=login("editor");
        mvc.perform(get("/players")).andExpect(content().string(not(containsString("非公開氏名"))));
        mvc.perform(get("/players/{id}/edit",p.getId()).session(session)).andExpect(content().string(not(containsString("非公開氏名")))).andExpect(content().string(not(containsString("name=\"hits\""))));
        mvc.perform(get("/api/input").session(session).param("year","2026").param("month","2026-09")).andExpect(content().string(not(containsString("非公開氏名")))).andExpect(jsonPath("$.players[0].hits").doesNotExist()).andExpect(jsonPath("$.players[0].average").doesNotExist());
        mvc.perform(post("/players/{id}/edit",p.getId()).session(session).with(csrf()).param("version",p.getVersion().toString()).param("name","改ざん").param("hits","9").param("atBats","10")).andExpect(redirectedUrl("/players"));
        var saved=players.findById(p.getId()).orElseThrow();assertThat(saved.getName()).isEqualTo("非公開氏名");assertThat(saved.getHits()).isEqualTo(3);
        mvc.perform(post("/api/input/stats").session(session).with(csrf()).param("playerId",p.getId().toString()).param("hits","9").param("version",saved.getVersion().toString())).andExpect(status().isForbidden());
        mvc.perform(get("/players/{id}/edit",p.getId()).session(login("admin"))).andExpect(content().string(containsString("非公開氏名")));
    }
    @Test void onlyMasterCanHidePlayersAndHistoryIsRetained() throws Exception {
        var p=player("表示切替",1,1,"投手");var session=login("editor");var master=login("admin");var u=(UserSession)master.getAttribute("userSession");inputService.fee(u,p.getId(),2026,true,"支払済み",-1L);
        mvc.perform(post("/players/{id}/visibility",p.getId()).session(session).with(csrf()).param("visible","false")).andExpect(status().isForbidden());
        mvc.perform(post("/players/{id}/visibility",p.getId()).session(master).with(csrf()).param("visible","false")).andExpect(status().isOk());
        assertThat(players.findById(p.getId()).orElseThrow().getDeleteFlg()).isEqualTo(1);assertThat(annualFees.count()).isEqualTo(1);
        mvc.perform(get("/api/input").session(session).param("year","2026").param("month","2026-09")).andExpect(jsonPath("$.players.length()").value(0));
        inputService.visible(u,p.getId(),true);assertThat(players.findActivePlayers()).hasSize(1);
    }
    @Test void autosaveStatsAndGearValidateAndRejectConflicts() throws Exception {
        var p=player("入力選手",1,1,"投手");var u=(UserSession)login("editor").getAttribute("userSession");
        var version=inputService.stats(u,p.getId(),20,5,p.getVersion());assertThat(players.findById(p.getId()).orElseThrow().getHits()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->inputService.stats(u,p.getId(),20,6,p.getVersion())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var result=inputService.gear(u,null,"バット",p.getId(),"木製",-1L,false);Long id=(Long)result.get("id");
        inputService.gear(u,id,"バット",null,"共有",(Long)result.get("version"),false);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->inputService.gear(u,id,"",null,"",(Long)result.get("version"),true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var g=gears.findById(id).orElseThrow();inputService.gear(u,id,"",null,"",g.getVersion(),true);assertThat(gears.count()).isZero();
    }
    @Test void manualDatesRemainSortedUniqueAndYearLimited() throws Exception {
        var u=(UserSession)login("editor").getAttribute("userSession");inputService.date(u,LocalDate.of(2026,9,19));inputService.date(u,LocalDate.of(2026,9,16));
        holidayService.addHoliday(LocalDate.of(2026,9,21),"祝日");
        assertThat(attendanceService.dates(java.time.YearMonth.of(2026,9))).isSorted().doesNotHaveDuplicates().contains(LocalDate.of(2026,9,16),LocalDate.of(2026,9,21));
        org.assertj.core.api.Assertions.assertThatThrownBy(()->inputService.date(u,LocalDate.of(2101,1,1))).isInstanceOf(IllegalArgumentException.class);
    }
}

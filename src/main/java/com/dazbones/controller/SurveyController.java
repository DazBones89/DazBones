package com.dazbones.controller;

import com.dazbones.service.SurveyService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
public class SurveyController {

    private final SurveyService service;

    public SurveyController(SurveyService service) {
        this.service = service;
    }

    @GetMapping("/api/survey")
    public List<Map<String,Object>> get(@RequestParam String start,
                                        @RequestParam String end) {

        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);

        List<Map<String,Object>> list = new ArrayList<>();

        while(!s.isAfter(e)) {

            Map<String,Object> data = service.getSummary(s);

            Map<String,Object> event = new HashMap<>();
            event.put("start", s.toString());
            event.put("title",
                    "参:"+data.get("参加")+" 不:"+data.get("不参加"));

            String type = (String)data.get("type");

            if ("auto".equals(type)) event.put("color","#fde68a");
            else if ("manual".equals(type)) event.put("color","#7dd3fc");
            else if ("mixed".equals(type)) event.put("color","#facc15");
            else event.put("color","#ffffff");

            list.add(event);

            s = s.plusDays(1);
        }

        return list;
    }

    @PostMapping("/survey/answer")
    public void answer(@RequestParam String date,
                       @RequestParam Long memberId,
                       @RequestParam String status) {

        service.answer(LocalDate.parse(date), memberId, status);
    }
}
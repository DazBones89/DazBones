package com.dazbones.model;

import jakarta.validation.constraints.*;

public class PlayerForm {

    @NotBlank(message = "名前は必須です")
    @Size(max = 100, message = "名前は100文字以内で入力してください")
    private String name;

    @Size(max = 50, message = "ポジションは50文字以内で入力してください")
    private String position;

    @Min(value = 0, message = "背番号は0以上で入力してください")
    @Max(value = 99, message = "背番号は99以下で入力してください")
    private Integer number;

    @Size(max = 1000, message = "コメントは1000文字以内で入力してください")
    private String comment;

    private Integer id;

    // getter/setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
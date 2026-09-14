package com.dazbones.model;
import jakarta.persistence.*;
@Entity @Table(name="security_state")
public class SecurityState {
    @Id private Integer id = 1;
    private long generation;
    public Integer getId(){return id;}
    public long getGeneration(){return generation;}
    public void advance(){generation++;}
}

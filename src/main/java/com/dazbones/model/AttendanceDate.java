package com.dazbones.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="attendance_dates")
public class AttendanceDate {
 @Id @Column(name="target_date") public LocalDate date;
 public AttendanceDate(){} public AttendanceDate(LocalDate date){this.date=date;}
}

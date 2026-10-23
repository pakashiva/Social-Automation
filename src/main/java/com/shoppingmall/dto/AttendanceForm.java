package com.shoppingmall.dto;


import com.shoppingmall.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AttendanceForm {
    @NotNull
    private Long employeeId;
    @NotNull
    private LocalDate workDate;
    @NotNull
    private AttendanceStatus status;
    private LocalTime checkIn;
    private LocalTime checkOut;
    @Size(max = 255)
    private String note;
}

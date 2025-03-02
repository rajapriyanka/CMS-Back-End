package com.cms.repository;

import com.cms.entities.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDayOrderByPeriodNumber(DayOfWeek day);
    List<TimeSlot> findByDayAndIsBreakFalseOrderByPeriodNumber(DayOfWeek day);
    List<TimeSlot> findByPeriodNumberAndIsBreakFalse(Integer periodNumber);
    List<TimeSlot> findAllByIsBreakFalseOrderByDayAscPeriodNumberAsc();
}


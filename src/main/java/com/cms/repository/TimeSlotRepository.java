package com.cms.repository;

import com.cms.entities.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDayOrderByPeriodNumber(DayOfWeek day);
    List<TimeSlot> findByDayAndIsBreakFalseOrderByPeriodNumber(DayOfWeek day);
    List<TimeSlot> findByPeriodNumberAndIsBreakFalse(Integer periodNumber);
    List<TimeSlot> findAllByIsBreakFalseOrderByDayAscPeriodNumberAsc();
    List<TimeSlot> findByDay(DayOfWeek day);
    
    List<TimeSlot> findByDayAndIsBreak(DayOfWeek day, Boolean isBreak);
    
    Optional<TimeSlot> findByDayAndPeriodNumber(DayOfWeek day, Integer periodNumber);
    
    List<TimeSlot> findByIsBreak(Boolean isBreak);
}


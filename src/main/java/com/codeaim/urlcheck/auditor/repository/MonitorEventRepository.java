package com.codeaim.urlcheck.auditor.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codeaim.urlcheck.auditor.model.MonitorEvent;

@Repository
public interface MonitorEventRepository extends JpaRepository<MonitorEvent, Long>
{
    @Query(" SELECT me " +
            "FROM MonitorEvent me " +
            "WHERE me.created <= :expiryDate " +
            "   AND (me.changed = false " +
            "       OR me.confirmation = false)"
    )
    List<MonitorEvent> findExpiredMonitorEvents(
            @Param("expiryDate") LocalDateTime expiryDate
    );
}


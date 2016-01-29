package com.codeaim.urlcheck.auditor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codeaim.urlcheck.auditor.model.MonitorEvent;

@Repository
public interface MonitorEventRepository extends JpaRepository<MonitorEvent, Long>
{
}


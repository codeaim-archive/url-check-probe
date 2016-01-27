package com.codeaim.urlcheck.auditor.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.codeaim.urlcheck.auditor.model.MonitorEvent;

@Repository
public interface MonitorEventRepository extends CrudRepository<MonitorEvent, String>
{
}


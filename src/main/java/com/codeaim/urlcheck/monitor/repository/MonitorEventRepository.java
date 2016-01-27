package com.codeaim.urlcheck.monitor.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.codeaim.urlcheck.monitor.model.MonitorEvent;

@Repository
public interface MonitorEventRepository extends CrudRepository<MonitorEvent, String>
{
}


package com.teamproject.japan_newhire_rag_backend.domain.system.error.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.entity.SystemErrorLog;

public interface SystemErrorLogRepository extends JpaRepository<SystemErrorLog, Long> {
}

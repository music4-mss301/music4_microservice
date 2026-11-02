package vn.edu.fpt.mss.engagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.mss.engagementservice.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}

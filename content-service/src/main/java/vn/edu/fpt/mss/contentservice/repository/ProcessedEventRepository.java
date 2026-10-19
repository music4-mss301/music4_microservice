package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.mss.contentservice.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}

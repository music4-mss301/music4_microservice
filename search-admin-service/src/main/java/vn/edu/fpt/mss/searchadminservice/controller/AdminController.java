package vn.edu.fpt.mss.searchadminservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.searchadminservice.dto.request.CurationRequest;
import vn.edu.fpt.mss.searchadminservice.dto.response.CurationResponse;
import vn.edu.fpt.mss.searchadminservice.dto.response.DashboardResponse;
import vn.edu.fpt.mss.searchadminservice.dto.response.ReindexStatus;
import vn.edu.fpt.mss.searchadminservice.service.AdminService;
import vn.edu.fpt.mss.searchadminservice.service.CurationService;
import vn.edu.fpt.mss.searchadminservice.service.IndexingService;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final IndexingService indexingService;
    private final CurationService curationService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @PostMapping("/search/reindex")
    public ResponseEntity<ReindexStatus> reindex() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(indexingService.startReindex());
    }

    @GetMapping("/search/status")
    public ReindexStatus reindexStatus() {
        return indexingService.status();
    }

    @PostMapping("/system/cleanup-history")
    public Map<String, Integer> cleanupHistory(@RequestParam(defaultValue = "90") int days) {
        return adminService.cleanupHistory(days);
    }

    // ------------------------------------------------ Curation CRUD
    @GetMapping("/curations")
    public List<CurationResponse> curations() {
        return curationService.getAll();
    }

    @GetMapping("/curations/{id}")
    public CurationResponse curation(@PathVariable String id) {
        return curationService.get(id);
    }

    @PostMapping("/curations")
    public ResponseEntity<CurationResponse> createCuration(@Valid @RequestBody CurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(curationService.create(request));
    }

    @PutMapping("/curations/{id}")
    public CurationResponse updateCuration(@PathVariable String id, @Valid @RequestBody CurationRequest request) {
        return curationService.update(id, request);
    }

    @DeleteMapping("/curations/{id}")
    public ResponseEntity<Void> deleteCuration(@PathVariable String id) {
        curationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

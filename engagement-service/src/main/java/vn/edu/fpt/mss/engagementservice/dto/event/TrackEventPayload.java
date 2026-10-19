package vn.edu.fpt.mss.engagementservice.dto.event;

import java.util.List;

/** Chỉ đọc các field cần dùng từ event content.track.*; field thừa bị bỏ qua. */
public record TrackEventPayload(Integer trackId, String name, String img, List<Ref> artists) {
    public record Ref(Integer id, String name) {
    }
}

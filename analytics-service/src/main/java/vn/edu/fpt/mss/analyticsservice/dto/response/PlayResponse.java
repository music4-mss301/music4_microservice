package vn.edu.fpt.mss.analyticsservice.dto.response;

/** counted=false khi bị bỏ qua do nghe lại cùng bài trong 30 giây (chống spam lượt nghe). */
public record PlayResponse(Integer trackId, boolean counted, String message) {
}

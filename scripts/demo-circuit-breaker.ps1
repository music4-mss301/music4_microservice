# ==============================================================================
# Script Kiểm thử & Demo Kịch bản Circuit Breaker (Resilience4j)
# Phụ trách: Nguyễn Tấn Quốc (CE190614) - Music4 Microservices
# ==============================================================================

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "DEMO CIRCUIT BREAKER (RESILIENCE4J) - MSS301" -ForegroundColor Yellow
Write-Host "Phụ trách: Nguyễn Tấn Quốc (CE190614)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$GATEWAY_SECRET = "54d68086ce267ea24ba6d2fe82fe0645fb74d8c0aa1bdf08"
$HEADERS = @{
    "X-Gateway-Secret" = $GATEWAY_SECRET
}

# 1. Kiểm tra Actuator Circuit Breakers của search-admin-service
Write-Host "`n[1] Kiểm tra trạng thái Circuit Breakers qua Actuator (Port 8086)..." -ForegroundColor White
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8086/actuator/health" -Method Get -TimeoutSec 3
    Write-Host "  -> Health Status: $($health.status)" -ForegroundColor Green
    if ($health.components.circuitBreakers) {
        Write-Host "  -> Circuit Breakers Component: $($health.components.circuitBreakers.status)" -ForegroundColor Green
    }
} catch {
    Write-Host "  -> [CHÚ Ý] search-admin-service chưa bật tại http://localhost:8086" -ForegroundColor Red
}

# 2. Thử nghiệm Kịch bản Search: Elasticsearch -> Fallback Database
Write-Host "`n[2] Kiểm tra Tìm kiếm với Circuit Breaker 'elasticsearch'..." -ForegroundColor White
try {
    $searchRes = Invoke-RestMethod -Uri "http://localhost:8086/api/search?q=pop&type=all" -Headers $HEADERS -Method Get -TimeoutSec 3
    Write-Host "  -> Query: $($searchRes.query)" -ForegroundColor Green
    Write-Host "  -> Nguồn kết quả (source): $($searchRes.source)" -ForegroundColor Yellow
    if ($searchRes.source -eq "elasticsearch") {
        Write-Host "  => [OK] Circuit Breaker đang CLOSED, dữ liệu lấy trực tiếp từ Elasticsearch." -ForegroundColor Green
    } elseif ($searchRes.source -eq "database") {
        Write-Host "  => [FALLBACK KÍCH HOẠT] Elasticsearch gặp sự cố, Circuit Breaker đã chuyển sang dùng DB của content-service!" -ForegroundColor Magenta
    }
} catch {
    Write-Host "  -> Lỗi gọi API search: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Thử nghiệm Kịch bản Internal Recommendation (kNN Vector)
Write-Host "`n[3] Kiểm tra Internal Recommendation (kNN Vector)..." -ForegroundColor White
try {
    $recBody = '{"likedTrackIds":[1, 2],"size":5}'
    $recHeaders = @{
        "Content-Type" = "application/json"
        "X-Gateway-Secret" = $GATEWAY_SECRET
        "X-Internal-Token" = "7cdfdb0fb364b214c4d7d922130abbc272dae997dca932e5"
    }
    $recRes = Invoke-RestMethod -Uri "http://localhost:8086/internal/search/recommend" -Headers $recHeaders -Body $recBody -Method Post -TimeoutSec 3
    Write-Host "  -> Gọi thành công /internal/search/recommend (Sẵn sàng phục vụ analytics-service)" -ForegroundColor Green
} catch {
    Write-Host "  -> Lỗi gọi internal recommend: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "HƯỚNG DẪN THỰC HÀNH TẮT / BẬT ĐỂ DEMO:" -ForegroundColor Yellow
Write-Host "1. Tắt Elasticsearch: docker stop music4-elasticsearch" -ForegroundColor White
Write-Host "   Sau đó chạy lại script này: Nguồn kết quả sẽ tự động đổi sang 'database'!" -ForegroundColor Gray
Write-Host "2. Bật lại Elasticsearch: docker start music4-elasticsearch" -ForegroundColor White
Write-Host "   Sau vài giây chạy lại script: Nguồn kết quả sẽ tự động quay về 'elasticsearch'!" -ForegroundColor Gray
Write-Host "3. Tắt search-admin-service (Port 8086) và vào trang Gợi ý bài hát của FE:" -ForegroundColor White
Write-Host "   analytics-service sẽ tự động chuyển sang danh sách bài hát Trending 7 ngày!" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan

package vn.edu.fpt.sba.music4.services.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // ConcurrentHashMap để lưu danh sách xô token an toàn trong môi trường đa luồng (Thread-safe)
    private final Map<String, Bucket> emailBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();


    public Bucket resolveEmailBucket(String email) {
        return emailBuckets.computeIfAbsent(email, key -> {
            Bandwidth limit = Bandwidth.classic(1, Refill.intervally(1, Duration.ofSeconds(60)));
            return Bucket.builder().addLimit(limit).build();
        });
    }
    public Bucket resolveIpBucket(String ip) {
        return ipBuckets.computeIfAbsent(ip, key -> {
            Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(15)));
            return Bucket.builder().addLimit(limit).build();
        });
    }
}
package vn.edu.fpt.mss.searchadminservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * esUrl: http://elasticsearch:9200. aiUrl: http://ai-service:8000 (để trống = tắt embedding).
 * vectorDims phải khớp model của AI-service (all-MiniLM-L6-v2 = 384).
 */
@ConfigurationProperties(prefix = "app.search")
public record SearchProperties(String esUrl, String esUsername, String esPassword,
                               String aiUrl, String aiApiKey, int vectorDims) {
    public SearchProperties {
        vectorDims = vectorDims <= 0 ? 384 : vectorDims;
    }
}

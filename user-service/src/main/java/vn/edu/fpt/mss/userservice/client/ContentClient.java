package vn.edu.fpt.mss.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.edu.fpt.mss.userservice.dto.client.ArtistProfileResponse;
import vn.edu.fpt.mss.userservice.dto.client.CreateArtistProfileRequest;

/** url rỗng => gọi qua Eureka (lb://content-service); profile render => URL tĩnh. */
@FeignClient(name = "content-service", url = "${services.content-service.url:}",
        fallbackFactory = ContentClientFallbackFactory.class)
public interface ContentClient {

    @PostMapping("/internal/artists")
    ArtistProfileResponse createArtistProfile(@RequestBody CreateArtistProfileRequest request);
}

package vn.edu.fpt.mss.searchadminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.fpt.mss.searchadminservice.dto.client.StatsDtos;

@FeignClient(name = "user-service", url = "${services.user-service.url:}",
        fallbackFactory = AdminClientsFallbacks.User.class)
public interface UserClient {

    @GetMapping("/internal/users/stats")
    StatsDtos.UserStats stats();
}

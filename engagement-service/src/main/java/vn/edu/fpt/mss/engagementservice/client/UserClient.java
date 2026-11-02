package vn.edu.fpt.mss.engagementservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.mss.engagementservice.dto.client.UserSummary;

import java.util.List;

@FeignClient(name = "user-service", url = "${services.user-service.url:}",
        fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/internal/users")
    List<UserSummary> getUsers(@RequestParam("ids") List<Integer> ids);
}

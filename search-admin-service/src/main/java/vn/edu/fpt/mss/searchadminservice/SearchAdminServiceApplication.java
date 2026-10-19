package vn.edu.fpt.mss.searchadminservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Không có SQL DB: kho dữ liệu riêng là Elasticsearch (index music_* và search_curations). */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class SearchAdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchAdminServiceApplication.class, args);
    }
}

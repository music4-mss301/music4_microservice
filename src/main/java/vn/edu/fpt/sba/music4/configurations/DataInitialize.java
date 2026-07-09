package vn.edu.fpt.sba.music4.configurations;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.fpt.sba.music4.entities.Provider;
import vn.edu.fpt.sba.music4.repositories.ProviderRepository;

import java.util.List;

@Configuration
public class DataInitialize {
    @Bean
    CommandLineRunner initDatabase(ProviderRepository providerRepository) {
        return args -> {
            if (providerRepository.count() == 0) {
                Provider googleProvider = new Provider();
                googleProvider.setName("Google");

                Provider localProvider = new Provider();
                localProvider.setName("Local");

                providerRepository.saveAll(List.of(googleProvider, localProvider));
            }
        };
    }
}

package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Provider;
import vn.edu.fpt.sba.music4.entities.UserProvider;

import java.util.Optional;

@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, Integer> {
    Optional<UserProvider> findByProviderUserIdAndProvider(String providerUserId, Provider provider);
}

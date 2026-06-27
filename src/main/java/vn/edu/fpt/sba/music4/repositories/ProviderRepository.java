package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Provider;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    Optional<Provider> findByName(String name);
}

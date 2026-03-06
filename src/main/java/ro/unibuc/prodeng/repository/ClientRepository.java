package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.ClientEntity;

import java.util.Optional;

@Repository
public interface ClientRepository extends MongoRepository<ClientEntity, String> {
    Optional<ClientEntity> findByEmail(String email);
}

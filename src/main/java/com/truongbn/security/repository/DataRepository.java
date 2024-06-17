package com.truongbn.security.repository;

import com.truongbn.security.entities.DataEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DataRepository extends JpaRepository<DataEntity, Integer> {

    @Query("SELECT d FROM DataEntity d WHERE d.user.userId = :userId")
    Optional<DataEntity> findByUserId(String userId);
}

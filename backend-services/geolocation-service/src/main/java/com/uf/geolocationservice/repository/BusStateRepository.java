package com.uf.geolocationservice.repository;

import com.uf.geolocationservice.model.BusStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BusStateRepository extends JpaRepository<BusStateEntity, String> {
    List<BusStateEntity> findByLastUpdatedAtAfter(Instant since);
}

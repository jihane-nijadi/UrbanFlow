package com.uf.geolocationservice.service;

import com.uf.geolocationservice.model.BusStateEntity;
import com.uf.geolocationservice.model.BusUpdate;
import com.uf.geolocationservice.model.BusState;
import com.uf.geolocationservice.repository.BusStateRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "geolocation.persistence", name = "enabled", havingValue = "true")
public class PersistentBusStore implements BusStore {

    private final BusStateRepository repository;
    private final int maxHistory = 50;

    public PersistentBusStore(BusStateRepository repository) {
        this.repository = repository;
    }

    public void upsert(BusUpdate update) {
        Instant now = Instant.now();
        BusStateEntity entity = new BusStateEntity(
                update.getBusId(),
                update.getLineId(),
                update.getLat(),
                update.getLon(),
                now
        );
        repository.save(entity);
    }

    public Optional<BusState> get(String busId) {
        return repository.findById(busId).map(e -> {
            BusState s = new BusState(e.getBusId(), e.getLineId(), maxHistory);
            // rebuild a BusUpdate to populate the BusState recent positions and lastUpdatedAt
            BusUpdate u = new BusUpdate();
            u.setBusId(e.getBusId());
            u.setLineId(e.getLineId());
            if (e.getLat() != null) u.setLat(e.getLat());
            if (e.getLon() != null) u.setLon(e.getLon());
            if (e.getLastUpdatedAt() != null) u.setTs(e.getLastUpdatedAt().toEpochMilli());
            s.apply(u);
            return s;
        });
    }

    public List<BusState> listActive() {
        Instant since = Instant.now().minusSeconds(60);
        List<BusStateEntity> entities = repository.findByLastUpdatedAtAfter(since);
        return entities.stream().map(e -> {
            BusState s = new BusState(e.getBusId(), e.getLineId(), maxHistory);
            BusUpdate u = new BusUpdate();
            u.setBusId(e.getBusId());
            u.setLineId(e.getLineId());
            if (e.getLat() != null) u.setLat(e.getLat());
            if (e.getLon() != null) u.setLon(e.getLon());
            if (e.getLastUpdatedAt() != null) u.setTs(e.getLastUpdatedAt().toEpochMilli());
            s.apply(u);
            return s;
        }).collect(Collectors.toList());
    }
}

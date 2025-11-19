package com.uf.geolocationservice.service;

import com.uf.geolocationservice.model.BusState;
import com.uf.geolocationservice.model.BusUpdate;

import java.util.List;
import java.util.Optional;

public interface BusStore {
    void upsert(BusUpdate update);

    Optional<BusState> get(String busId);

    List<BusState> listActive();
}

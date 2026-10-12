package com.factory.automation.domain.repository;

import com.factory.automation.domain.enums.StationStatus;
import com.factory.automation.domain.enums.StationType;
import com.factory.automation.domain.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByStationCode(String stationCode);
    List<Station> findByStationType(StationType stationType);
    List<Station> findByStatus(StationStatus status);
}

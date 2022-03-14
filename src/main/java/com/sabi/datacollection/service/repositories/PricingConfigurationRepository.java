package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.PricingConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingConfigurationRepository extends JpaRepository<PricingConfiguration, Long> {
    Page<PricingConfiguration> findPricingConfigurationByDataSetId(Long dataSetId, Pageable pageable);

    List<PricingConfiguration> findPricingConfigurationByIsActive(Boolean isActive);

    Page<PricingConfiguration> findAllBy(Pageable pageable);
}

package com.nrall.ceibatest.service;

import com.nrall.ceibatest.domain.model.Fund;

import java.util.List;
import java.util.Optional;

public interface FundCatalogService {

    List<Fund> getAll();

    Optional<Fund> findById(Integer id);
}


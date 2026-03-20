package com.nrall.ceibatest.service.impl;

import com.nrall.ceibatest.domain.model.Fund;
import com.nrall.ceibatest.domain.model.FundCategory;
import com.nrall.ceibatest.service.FundCatalogService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class StaticFundCatalogService implements FundCatalogService {

    private static final List<Fund> FUNDS = List.of(
            new Fund(1, "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), FundCategory.FPV),
            new Fund(2, "FPV_BTG_PACTUAL_ECOPETROL", new BigDecimal("125000"), FundCategory.FPV),
            new Fund(3, "DEUDAPRIVADA", new BigDecimal("50000"), FundCategory.FIC),
            new Fund(4, "FDO-ACCIONES", new BigDecimal("250000"), FundCategory.FIC),
            new Fund(5, "FPV_BTG_PACTUAL_DINAMICA", new BigDecimal("100000"), FundCategory.FPV)
    );

    @Override
    public List<Fund> getAll() {
        return FUNDS;
    }

    @Override
    public Optional<Fund> findById(Integer id) {
        return FUNDS.stream().filter(fund -> fund.id().equals(id)).findFirst();
    }
}


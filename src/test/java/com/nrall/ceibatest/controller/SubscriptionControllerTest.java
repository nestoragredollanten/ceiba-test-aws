package com.nrall.ceibatest.controller;

import tools.jackson.databind.ObjectMapper;
import com.nrall.ceibatest.config.TestRepositoryConfig;
import com.nrall.ceibatest.domain.model.NotificationChannel;
import com.nrall.ceibatest.dto.SubscribeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRepositoryConfig.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/funds"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListFundsForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/funds")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("cliente", "cliente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("FPV_BTG_PACTUAL_RECAUDADORA"));
    }

    @Test
    void shouldCreateSubscription() throws Exception {
        SubscribeRequest request = new SubscribeRequest(
                "CUST-API-01",
                1,
                new BigDecimal("75000"),
                NotificationChannel.EMAIL,
                "cliente@correo.com"
        );

        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("cliente", "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fundName").value("FPV_BTG_PACTUAL_RECAUDADORA"));
    }
}


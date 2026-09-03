package com.verilogic.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End DDoS Token-Bucket Rate Limiter Integration Test.
 * Tests burst capacity limits, volumetric throttle enforcement,
 * and standard rate limit headers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class DdosRateLimiterFilterE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("DDoS filter emits X-RateLimit headers and throttles when burst capacity is exceeded")
    void testVolumetricRateLimiting() throws Exception {
        String testIp = "203.0.113.42";

        // Validate baseline headers on normal request
        mockMvc.perform(get("/api/v1/health").header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "100"))
                .andExpect(header().exists("X-RateLimit-Remaining"));

        // Rapid volumetric burst until token bucket is exhausted
        boolean rateLimited = false;
        MvcResult throttledResult = null;

        for (int i = 0; i < 300; i++) {
            MvcResult result = mockMvc.perform(get("/api/v1/health").header("X-Forwarded-For", testIp)).andReturn();
            if (result.getResponse().getStatus() == 429) {
                rateLimited = true;
                throttledResult = result;
                break;
            }
        }

        assertThat(rateLimited).as("IP should be rate-limited after volumetric burst").isTrue();
        assertThat(throttledResult).isNotNull();
        assertThat(throttledResult.getResponse().getHeader("Retry-After")).isEqualTo("1");
        assertThat(throttledResult.getResponse().getContentAsString()).contains("DDOS_RATE_LIMIT_EXCEEDED");
    }
}

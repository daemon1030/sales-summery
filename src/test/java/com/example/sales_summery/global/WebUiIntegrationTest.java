package com.example.sales_summery.global;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebUiIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void testPageIsAvailableWithoutAuthentication() throws Exception {
        // 로그인 전에도 서비스 진입점과 별도로 분리한 API 테스트 화면을 불러올 수 있어야 한다.
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/service/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales Summary")));
        mockMvc.perform(get("/api-test/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales Summery API Tester")));
        mockMvc.perform(get("/api-test/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/api-test/styles.css")).andExpect(status().isOk());
    }
}

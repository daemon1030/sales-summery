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
        // 로그인 전에도 테스트 화면과 정적 파일을 불러올 수 있어야 한다.
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales Summery API Tester")));
        mockMvc.perform(get("/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/styles.css")).andExpect(status().isOk());
    }
}

package com.example.sales_summery.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 날짜 계산이 실행 환경의 기본 시간대에 흔들리지 않도록 한국 시간 Clock을 제공한다.
@Configuration
public class DateTimeConfig {
    public static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(KOREA_ZONE_ID);
    }
}

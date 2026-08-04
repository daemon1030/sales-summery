package com.example.sales_summery.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sales_summery.global.config.DateTimeConfig;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.GlobalExceptionHandler;
import com.example.sales_summery.global.exception.UserNotFoundException;
import com.example.sales_summery.global.request.DateRangeRequest;
import com.example.sales_summery.global.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CommonApiFoundationTest {

    @Autowired
    private Validator validator;

    @Autowired
    private Clock clock;

    // 성공 응답에는 data만 있고 오류 정보는 없어야 한다.
    @Test
    void successResponseContainsData() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.error()).isNull();
    }

    // 도메인 예외가 약속된 상태 코드와 오류 코드로 변환되는지 확인한다.
    @Test
    void businessExceptionUsesCommonErrorResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(new UserNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.USER_NOT_FOUND.name());
    }

    // 공통 기간 DTO는 시작일이 종료일보다 늦은 요청을 차단해야 한다.
    @Test
    void invalidDateRangeIsRejected() {
        DateRangeRequest request = new DateRangeRequest(
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 1)
        );

        Set<ConstraintViolation<DateRangeRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("시작일은 종료일보다 늦을 수 없습니다.");
    }

    // 서비스의 날짜 계산은 운영체제 설정과 관계없이 한국 시간을 사용해야 한다.
    @Test
    void commonClockUsesKoreaTimeZone() {
        assertThat(clock.getZone()).isEqualTo(DateTimeConfig.KOREA_ZONE_ID);
    }
}

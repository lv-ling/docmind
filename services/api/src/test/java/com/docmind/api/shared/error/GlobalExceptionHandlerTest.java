package com.docmind.api.shared.error;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.docmind.api.shared.web.RequestIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private static final String UUID_PATTERN =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestIdFilter())
            .build();
  }

  @Test
  void returnsTheContractErrorShapeForValidationFailures() throws Exception {
    String requestId = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post("/test/validate")
                .header("X-Request-ID", requestId)
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("X-Request-ID", requestId))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.category").value("validation"))
        .andExpect(jsonPath("$.field_errors[0].path").value("name"))
        .andExpect(jsonPath("$.request_id").value(requestId))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void returnsSafeApplicationErrors() throws Exception {
    mockMvc
        .perform(get("/test/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
        .andExpect(jsonPath("$.category").value("conflict"))
        .andExpect(jsonPath("$.request_id").value(matchesPattern(UUID_PATTERN)));
  }

  @Test
  void doesNotExposeUnexpectedExceptionMessages() throws Exception {
    mockMvc
        .perform(get("/test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("服务暂时无法处理该请求"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("PII: 13800138000")));
  }

  @RestController
  static class TestController {

    @PostMapping("/test/validate")
    void validate(@Valid @RequestBody TestRequest request) {}

    @GetMapping("/test/conflict")
    void conflict() {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ApiErrorCode.VERSION_CONFLICT,
          ApiErrorCategory.CONFLICT,
          "版本已被其他操作更新");
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
      throw new IllegalStateException("PII: 13800138000");
    }
  }

  record TestRequest(@NotBlank(message = "名称不能为空") String name) {}
}

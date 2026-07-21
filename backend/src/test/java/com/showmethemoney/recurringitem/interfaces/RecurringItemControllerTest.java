package com.showmethemoney.recurringitem.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showmethemoney.auth.infrastructure.JwtAuthenticationFilter;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.config.SecurityConfig;
import com.showmethemoney.recurringitem.application.RecurringItemService;
import com.showmethemoney.recurringitem.interfaces.dto.CreateRecurringItemRequest;
import com.showmethemoney.recurringitem.interfaces.dto.RecurringItemResponse;
import com.showmethemoney.recurringitem.interfaces.dto.UpdateRecurringItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecurringItemController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RecurringItemControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean RecurringItemService recurringItemService;

    private static UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void 고정항목_생성() throws Exception {
        CreateRecurringItemRequest request =
                new CreateRecurringItemRequest(0, "FOOD", "점심 구독", new BigDecimal("15000"), 15);

        mockMvc.perform(post("/api/recurring-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 고정항목_생성_유효성검사_실패() throws Exception {
        CreateRecurringItemRequest request =
                new CreateRecurringItemRequest(null, "", "", null, 32);

        mockMvc.perform(post("/api/recurring-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 고정항목_목록조회() throws Exception {
        given(recurringItemService.getList(1L, null, null)).willReturn(List.of(
                new RecurringItemResponse(1L, "0", "001", "식비", "점심 구독",
                        new BigDecimal("15000"), 15, 1)
        ));

        mockMvc.perform(get("/api/recurring-items").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("점심 구독"));
    }

    @Test
    void 고정항목_수정() throws Exception {
        UpdateRecurringItemRequest request =
                new UpdateRecurringItemRequest("저녁 구독", new BigDecimal("20000"), 20, null, true);

        mockMvc.perform(put("/api/recurring-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 고정항목_삭제() throws Exception {
        mockMvc.perform(delete("/api/recurring-items/1").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 인증없이_401() throws Exception {
        mockMvc.perform(get("/api/recurring-items"))
                .andExpect(status().isUnauthorized());
    }
}

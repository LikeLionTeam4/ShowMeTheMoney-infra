package com.showmethemoney.category.interfaces;

import com.showmethemoney.auth.infrastructure.JwtAuthenticationFilter;
import com.showmethemoney.auth.infrastructure.JwtTokenProvider;
import com.showmethemoney.category.application.CategoryService;
import com.showmethemoney.category.interfaces.dto.CategoryResponse;
import com.showmethemoney.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CategoryService categoryService;

    private static UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void 카테고리_전체조회() throws Exception {
        given(categoryService.getCategories(null)).willReturn(List.of(
                new CategoryResponse("FOOD", "001", "식비", 0),
                new CategoryResponse("SALARY", "101", "급여", 1)
        ));

        mockMvc.perform(get("/api/categories").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].code").value("FOOD"));
    }

    @Test
    void 카테고리_타입별조회() throws Exception {
        given(categoryService.getCategories(0)).willReturn(List.of(
                new CategoryResponse("FOOD", "001", "식비", 0)
        ));

        mockMvc.perform(get("/api/categories").param("type", "0").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value(0));
    }

    @Test
    void 카테고리_인증없이_401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }
}

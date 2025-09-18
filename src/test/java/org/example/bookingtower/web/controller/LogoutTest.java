package org.example.bookingtower.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.example.bookingtower.config.CustomUserDetailsService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {WebController.class, CustomErrorController.class})
public class LogoutTest {

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    public void testLogoutWithGetRequest() throws Exception {
        System.out.println("[DEBUG_LOG] Testing GET perform-logout request");

        mockMvc.perform(get("/perform-logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        System.out.println("[DEBUG_LOG] GET perform-logout test completed successfully");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testLogoutWithAdminUser() throws Exception {
        System.out.println("[DEBUG_LOG] Testing perform-logout with admin user");

        mockMvc.perform(get("/perform-logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        System.out.println("[DEBUG_LOG] Admin perform-logout test completed successfully");
    }
}

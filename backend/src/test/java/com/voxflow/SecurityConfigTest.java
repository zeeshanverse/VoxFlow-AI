package com.voxflow;

import com.voxflow.security.JwtAuthFilter;
import com.voxflow.security.JwtService;
import com.voxflow.security.SecurityConfig;
import com.voxflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        JwtAuthFilter.class,
        SecurityConfigTest.TestWebConfig.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(
                get("/api/test-protected")
        ).andExpect(
                status().isForbidden()
        );
    }

    @Test
    @WithMockUser(username = "test@voxflow.com")
    void protectedEndpointAllowsAuthenticatedRequest() throws Exception {
        mockMvc.perform(
                get("/api/test-protected")
        ).andExpect(
                status().isOk()
        );
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(
                get("/api/health")
        ).andExpect(
                status().isOk()
        );
    }

    @Configuration
    static class TestWebConfig {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/api/test-protected")
        String protectedEndpoint() {
            return "protected";
        }

        @GetMapping("/api/health")
        String health() {
            return "UP";
        }
    }
}
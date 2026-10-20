package net.matrix.gallery.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {"spring.security.user.name=admin", "spring.security.user.password=test-password"})
@AutoConfigureMockMvc
class AdminAuthenticationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void anonymousVisitorCannotOpenAdminLandingPage() throws Exception {
    mockMvc
        .perform(get("/admin").with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(unauthenticated());
  }

  @Test
  void anonymousBrowserVisitorIsRedirectedToLogin() throws Exception {
    mockMvc
        .perform(get("/admin").accept(MediaType.TEXT_HTML).with(anonymous()))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("http://localhost/login"))
        .andExpect(unauthenticated());
  }

  @Test
  void successfulLoginRedirectsToAdminLandingPage() throws Exception {
    mockMvc
        .perform(formLogin().user("admin").password("test-password"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/admin"))
        .andExpect(authenticated().withUsername("admin"));
  }

  @Test
  void authenticatedVisitorCanOpenAdminLandingPage() throws Exception {
    mockMvc
        .perform(get("/admin").with(user("admin")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Administration Tool")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("under construction")));
  }

  @Test
  void failedLoginReturnsToLoginPageWithError() throws Exception {
    mockMvc
        .perform(formLogin().user("admin").password("incorrect"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login?error"))
        .andExpect(unauthenticated());
  }

  @Test
  void categoriesRemainPublic() throws Exception {
    mockMvc
        .perform(get("/categories").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(unauthenticated());
  }

  @Test
  void galleryJavaScriptRemainsPublic() throws Exception {
    mockMvc
        .perform(get("/js/artwork-lightbox.js").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/javascript"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("dialog.showModal()")))
        .andExpect(unauthenticated());

    mockMvc
        .perform(get("/js/artwork-detail-lightbox.js").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/javascript"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("trigger.focus()")))
        .andExpect(unauthenticated());
  }
}

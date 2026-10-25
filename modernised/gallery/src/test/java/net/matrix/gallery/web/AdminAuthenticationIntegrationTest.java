package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "GALLERY_ADMIN_BOOTSTRAP_PASSWORD=test-bootstrap-password")
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
  void anonymousBrowserVisitorIsRedirectedToBrandedLogin() throws Exception {
    mockMvc
        .perform(get("/admin").accept(MediaType.TEXT_HTML).with(anonymous()))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("http://localhost/login"))
        .andExpect(unauthenticated());

    mockMvc
        .perform(get("/login").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Administration Tool")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("login-form")));
  }

  @Test
  void seededAdministratorAuthenticatesAndCanOpenAdminLandingPage() throws Exception {
    MvcResult loginResult =
        mockMvc
            .perform(formLogin().user("admin").password("test-bootstrap-password"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/admin"))
            .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"))
            .andReturn();

    mockMvc
        .perform(
            get("/admin").session((MockHttpSession) loginResult.getRequest().getSession(false)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Administration Tool")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("under construction")));

    mockMvc
        .perform(
            get("/admin/future")
                .session((MockHttpSession) loginResult.getRequest().getSession(false)))
        .andExpect(status().isNotFound());
  }

  @Test
  void authenticatedNonAdministratorCannotOpenAdminLandingPage() throws Exception {
    mockMvc
        .perform(get("/admin").with(user("editor").roles("USER")))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/admin/future").with(user("editor").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void incorrectCredentialsAreRejected() throws Exception {
    mockMvc
        .perform(formLogin().user("admin").password("incorrect"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login?error"))
        .andExpect(unauthenticated());
  }

  @Test
  void generatedDevelopmentUserIsNotUsable() throws Exception {
    mockMvc
        .perform(formLogin().user("user").password("test-bootstrap-password"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login?error"))
        .andExpect(unauthenticated());
  }

  @Test
  void logoutInvalidatesAuthenticatedSession() throws Exception {
    MvcResult loginResult =
        mockMvc
            .perform(formLogin().user("admin").password("test-bootstrap-password"))
            .andExpect(authenticated())
            .andReturn();
    MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

    mockMvc.perform(post("/logout").session(session).with(csrf())).andExpect(unauthenticated());

    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void categoriesAndGalleryJavaScriptRemainPublic() throws Exception {
    mockMvc
        .perform(get("/categories").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(unauthenticated());

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

  @Test
  void loginSubmissionWithoutCsrfTokenIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/login").param("username", "admin").param("password", "test-bootstrap-password"))
        .andExpect(status().isForbidden());
  }
}

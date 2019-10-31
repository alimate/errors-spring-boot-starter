package me.alidg.errors.adapter.attributes;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.conf.ServletErrorsAutoConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static me.alidg.Params.p;
import static me.alidg.errors.handlers.ServletWebErrorHandler.NO_HANDLER;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link ServletErrorAttributes}.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ServletErrorAttributesIT {

    /**
     * To perform web requests.
     */
    private MockMvc mvc;

    @Before
    public void setUp() {
        System.setProperty("errors.expose-arguments", "non_empty");
        String port = "--server.port=0";
        String messages = "--spring.messages.basename=test_messages";
        ConfigurableWebApplicationContext context =
            (ConfigurableWebApplicationContext) new SpringApplication(WebConfig.class).run(port, messages);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @After
    public void tearDown() {
        System.clearProperty("errors.expose-arguments");
    }

    @Test
    @Parameters(method = "dataForDifferentErrorScenarios")
    public void getErrorAttributes_ShouldExtractAndHandlesErrorsProperly(Throwable exception,
                                                                         int statusCode,
                                                                         int expectedStatusCode,
                                                                         String expectedErrorCodes,
                                                                         Argument argument) throws Exception {
        MockHttpServletRequestBuilder request = get("/error")
            .requestAttr("javax.servlet.error.status_code", statusCode)
            .requestAttr("javax.servlet.error.request_uri", "/test");

        if (exception != null) request.requestAttr("javax.servlet.error.exception", exception);

        ResultActions result = mvc.perform(request)
            .andExpect(status().is(expectedStatusCode))
            .andExpect(jsonPath("$.errors[0].code").value(expectedErrorCodes));

        if (argument != null) {
            String json = "$.errors[0].arguments." + argument.getName();
            result.andExpect(jsonPath(json).value(argument.getValue()));
        }
    }

    @Test
    public void getErrorAttributes_ShouldHandleNotFoundExceptionsWithoutPathArgAppropriately() throws Exception {
        MockHttpServletRequestBuilder request = get("/error")
            .requestAttr("javax.servlet.error.status_code", 404);

        mvc.perform(request)
            .andExpect(status().is(404))
            .andExpect(jsonPath("$.errors[0].code").value(NO_HANDLER))
            .andExpect(jsonPath("$.errors[0].arguments.path").value("unknown"));
    }

    private Object[] dataForDifferentErrorScenarios() {
        return p(
            p(new AccessDeniedException(""), 403, 403, ACCESS_DENIED, null),
            p(null, 403, 403, ACCESS_DENIED, null),
            p(null, 401, 401, AUTH_REQUIRED, null),
            p(null, 404, 404, NO_HANDLER, Argument.arg("path", "/test")),
            p(null, 3, 500, "unknown_error", null)
        );
    }

    @EnableWebMvc
    @TestConfiguration
    @ImportAutoConfiguration({
        ErrorsAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ErrorMvcAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        MessageSourceAutoConfiguration.class,
        ServletErrorsAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        ServletWebServerFactoryAutoConfiguration.class
    })
    protected static class WebConfig {
    }
}

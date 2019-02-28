package me.alidg.errors.adapter.attributes;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.conf.ServletErrorsAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static me.alidg.Params.p;
import static me.alidg.errors.handlers.ServletWebErrorHandler.NO_HANDLER;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
        String port = "--server.port=0";
        String messages = "--spring.messages.basename=test_messages";
        ConfigurableWebApplicationContext context =
                (ConfigurableWebApplicationContext) new SpringApplication(WebConfig.class).run(port, messages);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @Parameters(method = "dataForDifferentErrorScenarios")
    public void getErrorAttributes_ShouldExtractAndHandlesErrorsProperly(Throwable exception,
                                                                         int statusCode,
                                                                         int expectedStatusCode,
                                                                         String... expectedErrorCodes) throws Exception {
        MockHttpServletRequestBuilder request = get("/error")
                .requestAttr("javax.servlet.error.status_code", statusCode);

        if (exception != null) request.requestAttr("javax.servlet.error.exception", exception);

        mvc.perform(request)
                .andExpect(status().is(expectedStatusCode))
                .andExpect(jsonPath("$.errors[*].code").value(containsInAnyOrder(expectedErrorCodes)));
    }

    private Object[] dataForDifferentErrorScenarios() {
        return p(
                p(new AccessDeniedException(""), 0, 403, ACCESS_DENIED),
                p(null, 403, 403, ACCESS_DENIED),
                p(null, 401, 401, AUTH_REQUIRED),
                p(null, 404, 404, NO_HANDLER),
                p(null, 3, 500, "unknown_error")
        );
    }

    @EnableWebMvc
    @TestConfiguration
    @Import({
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
    protected static class WebConfig {}
}

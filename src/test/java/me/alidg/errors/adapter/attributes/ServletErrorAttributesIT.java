package me.alidg.errors.adapter.attributes;

import me.alidg.errors.Argument;
import me.alidg.errors.servlet.ServletApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static me.alidg.Params.p;
import static me.alidg.errors.handlers.ServletWebErrorHandler.NO_HANDLER;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link ServletErrorAttributes}.
 *
 * @author Ali Dehghani
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = ServletApplication.class, webEnvironment = RANDOM_PORT)
public class ServletErrorAttributesIT {
    /**
     * To perform web requests.
     */
    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @MethodSource("dataForDifferentErrorScenarios")
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

    private static Object[] dataForDifferentErrorScenarios() {
        return p(
            p(new AccessDeniedException(""), 0, 403, ACCESS_DENIED, null),
            p(new AccessDeniedException(""), 403, 403, ACCESS_DENIED, null),
            p(null, 403, 403, ACCESS_DENIED, null),
            p(null, 401, 401, AUTH_REQUIRED, null),
            p(null, 404, 404, NO_HANDLER, Argument.arg("path", "/test")),
            p(null, 3, 500, "unknown_error", null)
        );
    }
}
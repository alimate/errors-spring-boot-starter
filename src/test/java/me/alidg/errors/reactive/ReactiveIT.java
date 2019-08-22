package me.alidg.errors.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.ExceptionLogger;
import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.handlers.ServletWebErrorHandler;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static me.alidg.Params.p;
import static me.alidg.errors.handlers.LastResortWebErrorHandler.UNKNOWN_ERROR_CODE;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static me.alidg.errors.reactive.ReactiveController.Dto.dto;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

/**
 * Integration tests for our reactive stack support.
 *
 * @author Ali Dehghani
 */
@AutoConfigureWebTestClient
@RunWith(JUnitParamsRunner.class)
@SpringBootTest(classes = ReactiveApplication.class)
@TestPropertySource(properties = {
    "errors.add-fingerprint=false",
    "errors.expose-arguments=non_empty",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.main.web-application-type=reactive"
})
public class ReactiveIT {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private WebTestClient client;

    @MockBean
    private ExceptionLogger logger;

    @Test
    public void errorAttributes_ShouldBeAbleToHandleUnknownErrors() {
        client.mutateWith(csrf()).delete().uri("/test").exchange()
            .expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.errors[0].code").isEqualTo(UNKNOWN_ERROR_CODE)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldBeAbleToHandleAnnotatedExceptions() {
        client.get().uri("/test").exchange()
            .expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
            .expectBody()
            .jsonPath("$.errors[0].code").isEqualTo("invalid_params")
            .jsonPath("$.errors[0].message").isEqualTo("Params are: a, c and 10")
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.a").isEqualTo("a")
            .jsonPath("$.errors[0].arguments.c").isEqualTo("c")
            .jsonPath("$.errors[0].arguments.f").isEqualTo("10");

        verify(logger).log(any(ReactiveController.InvalidParamsException.class));
    }

    @Test
    @Parameters(method = "provideInvalidBody")
    public void errorAttributes_ShouldHandleValidationErrorsProperly(Object body,
                                                                     Locale locale,
                                                                     CodedMessage... expectedErrors) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(body);
        Object[] codes = Stream.of(expectedErrors).map(CodedMessage::getCode).toArray();
        Object[] messages = Stream.of(expectedErrors).map(CodedMessage::getMessage).toArray();

        WebTestClient.RequestHeadersSpec<?> request = client.mutateWith(csrf())
            .post().uri("/test").contentType(APPLICATION_JSON_UTF8).syncBody(data);
        if (locale != null) request = request.header(ACCEPT_LANGUAGE, locale.toString());

        request
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[*].code").value(Matchers.containsInAnyOrder(codes))
            .jsonPath("errors[*].message").value(Matchers.containsInAnyOrder(messages))
            .jsonPath("$.fingerprint").doesNotExist();

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleHandlerNotFoundSituations() {
        client.get().uri("/should_not_be_found").exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(NO_HANDLER)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.path").isEqualTo("/should_not_be_found");

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingBodiesProperly() {
        client.mutateWith(csrf()).post().uri("/test").contentType(APPLICATION_JSON_UTF8).exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(INVALID_OR_MISSING_BODY)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleInvalidBodiesProperly() {
        client.mutateWith(csrf()).post().uri("/test").contentType(APPLICATION_JSON_UTF8).syncBody("gibberish").exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(INVALID_OR_MISSING_BODY)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingContentTypesProperly() {
        client.mutateWith(csrf()).post().uri("/test").syncBody("gibberish").exchange()
            .expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(NOT_SUPPORTED)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments").exists();

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleInvalidContentTypesProperly() {
        client.mutateWith(csrf()).post().uri("/test").contentType(new MediaType("text", "gibberish")).syncBody("gibberish").exchange()
            .expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
            .expectBody().jsonPath("errors[0].code").isEqualTo(NOT_SUPPORTED);

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleInvalidAcceptHeadersProperly() {
        client.get().uri("/test/param?name=ali").accept(IMAGE_JPEG).exchange()
            .expectStatus().isEqualTo(NOT_ACCEPTABLE);

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleInvalidMethodsProperly() {
        client.mutateWith(csrf()).put().uri("/test").exchange()
            .expectStatus().isEqualTo(METHOD_NOT_ALLOWED)
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(ServletWebErrorHandler.METHOD_NOT_ALLOWED)
            .jsonPath("errors[0].message").isEqualTo("PUT method is not supported")
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.method").isEqualTo("PUT");

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingParametersProperly() {
        client.get().uri("/test/param").exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(MISSING_PARAMETER)
            .jsonPath("errors[0].message").isEqualTo("Parameter name of type String is required")
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.name").isEqualTo("name")
            .jsonPath("$.errors[0].arguments.expected").isEqualTo("String");

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingPartsProperly() {
        client.mutateWith(csrf()).post().uri("/test/part")
            .contentType(MULTIPART_FORM_DATA).syncBody(generateBody()).exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(MISSING_PART)
            .jsonPath("errors[0].message").isEqualTo("file part is required")
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.name").isEqualTo("file");

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleHandleUnauthorizedErrorsProperly() {
        client.get().uri("/test/protected").exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(AUTH_REQUIRED)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleHandleAccessDeniedErrorsProperly() {
        client
            .mutateWith(mockUser())
            .post().uri("/test/protected").exchange()
            .expectStatus().isForbidden()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(ACCESS_DENIED)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingHeadersProperly() {
        client.get().uri("/test/header").exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(MISSING_HEADER)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.name").isEqualTo("name")
            .jsonPath("$.errors[0].arguments.expected").isEqualTo("String");

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingCookiesProperly() {
        client.get().uri("/test/cookie").exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(MISSING_COOKIE)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.name").isEqualTo("name")
            .jsonPath("$.errors[0].arguments.expected").isEqualTo("String");

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingMatrixVarsProperly() {
        client.get().uri("/test/matrix").exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[0].code").isEqualTo(MISSING_MATRIX_VARIABLE)
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[0].arguments.name").isEqualTo("name")
            .jsonPath("$.errors[0].arguments.expected").isEqualTo("String");

        verify(logger).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleBindingExceptionsProperly() {
        client.get().uri("/test/paged?page=nan&size=na&sort=invalid")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("errors[*].code").value(containsInAnyOrder(
            "binding.type_mismatch.page", "binding.type_mismatch.size", "binding.type_mismatch.sort"))
            .jsonPath("$.fingerprint").doesNotExist()
            .jsonPath("$.errors[*].arguments.property").value(containsInAnyOrder("page", "size", "sort"))
            .jsonPath("$.errors[*].arguments.expected").value(containsInAnyOrder("Integer", "Integer", "Sort"))
            .jsonPath("$.errors[*].arguments.invalid").value(containsInAnyOrder("nan", "na", "invalid"));
    }

    @Test
    public void errorAttributes_ShouldHandleTypeMismatchesAsExpected() {
        client.get().uri("/test/type-mismatch?number=invalid")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[0].code").isEqualTo("binding.type_mismatch.number")
            .jsonPath("$.errors[0].arguments.property").isEqualTo("number")
            .jsonPath("$.errors[0].arguments.invalid").isEqualTo("invalid")
            .jsonPath("$.errors[0].arguments.expected").isEqualTo("Integer");
    }

    private MultiValueMap<String, HttpEntity<?>> generateBody() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("fieldPart", "fieldValue");
        builder.part("fileParts", new ByteArrayResource("data".getBytes()));
        return builder.build();
    }

    private Object[] provideInvalidBody() {
        return p(
            p(Collections.emptyMap(), null, cm("text.required", "The text is required")),
            p(dto(null, 10, "code"), null, cm("text.required", "The text is required")),
            p(dto("", 10, "code"), null, cm("text.required", "The text is required")),
            //p(dto("", 10, "code"), new Locale("fa", "IR"), cm("text.required", "متن اجباری است")),
            p(dto("text", -1, "code"), null, cm("number.min", "The min is 0")),
            p(dto("text", 0), null, cm("range.limit", "Between 1 and 2")),
            p(
                dto("", -1),
                null,
                cm("range.limit", "Between 1 and 2"),
                cm("number.min", "The min is 0"),
                cm("text.required", "The text is required")
            )
        );
    }

    private CodedMessage cm(String code, String message) {
        return new CodedMessage(code, message, emptyList());
    }
}

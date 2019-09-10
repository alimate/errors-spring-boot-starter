package me.alidg.errors.servlet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HttpError;
import me.alidg.errors.WebErrorHandlerPostProcessor;
import me.alidg.errors.handlers.LastResortWebErrorHandler;
import me.alidg.errors.handlers.MultipartWebErrorHandler;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.WebRequest;

import java.util.*;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.alidg.Params.p;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static me.alidg.errors.servlet.ServletController.Dto.dto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
@SpringBootTest(classes = ServletApplication.class, webEnvironment = RANDOM_PORT)
public class ServletIT {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private WebErrorHandlerPostProcessor processor;

    @Test
    public void controllerAdvice_ShouldBeAbleToHandleUnknownErrors() throws Exception {
        mvc.perform(delete("/test"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errors[0].code").value(LastResortWebErrorHandler.UNKNOWN_ERROR_CODE))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments").isEmpty());
    }

    @Test
    public void controllerAdvice_ShouldBeAbleToHandleAnnotatedExceptions() throws Exception {
        mvc.perform(get("/test"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].code").value("invalid_params"))
            .andExpect(jsonPath("$.errors[0].message").value("Params are: a, c and 10"))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments.a").value("a"))
            .andExpect(jsonPath("$.errors[0].arguments.c").value("c"))
            .andExpect(jsonPath("$.errors[0].arguments.f").value("10"));
    }

    @Test
    @Parameters(method = "provideInvalidBody")
    public void controllerAdvice_ShouldHandleValidationErrorsProperly(
        Object body,
        Locale locale,
        HttpError.CodedMessage... expectedErrors) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(body);

        Object[] codes = Stream.of(expectedErrors).map(HttpError.CodedMessage::getCode).toArray();
        Object[] messages = Stream.of(expectedErrors).map(HttpError.CodedMessage::getMessage).toArray();
        mvc.perform(post("/test").locale(locale).contentType("application/json").content(data))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[*].code").value(containsInAnyOrder(codes)))
            .andExpect(jsonPath("errors[*].message").value(containsInAnyOrder(messages)))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments").exists());
    }

    @Test
    public void controllerAdvice_ShouldHandleHandlerNotFoundSituations() throws Exception {
        mvc.perform(get("/should_not_be_found"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingBodiesProperly() throws Exception {
        mvc.perform(post("/test").contentType("application/json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].code").value(INVALID_OR_MISSING_BODY))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments").isEmpty());
    }

    @Test
    public void controllerAdvice_ShouldHandleInvalidBodiesProperly() throws Exception {
        mvc.perform(post("/test").contentType("application/json").content("gibberish"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].code").value(INVALID_OR_MISSING_BODY))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments").isEmpty());
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingContentTypesProperly() throws Exception {
        mvc.perform(post("/test").content("gibberish"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("errors[0].code").value(NOT_SUPPORTED));
    }

    @Test
    public void controllerAdvice_ShouldHandleInvalidContentTypesProperly() throws Exception {
        mvc.perform(post("/test").contentType("text/gibberish").content("gibberish"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("errors[0].code").value(NOT_SUPPORTED))
            .andExpect(jsonPath("errors[0].message").value("text/gibberish;charset=UTF-8 is not supported"));
    }

    @Test
    public void controllerAdvice_ShouldHandleInvalidAcceptHeadersProperly() throws Exception {
        mvc.perform(get("/test/param?name=ali").accept("image/jpeg"))
            .andExpect(status().isNotAcceptable());
    }

    @Test
    public void controllerAdvice_ShouldHandleInvalidMethodsProperly() throws Exception {
        mvc.perform(put("/test"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("errors[0].code").value(METHOD_NOT_ALLOWED))
            .andExpect(jsonPath("errors[0].message").value("PUT method is not supported"))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("errors[0].arguments.method").value("PUT"));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingParametersProperly() throws Exception {
        mvc.perform(get("/test/param"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].code").value(MISSING_PARAMETER))
            .andExpect(jsonPath("errors[0].message").value("Parameter name of type String is required"))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("errors[0].arguments.name").value("name"))
            .andExpect(jsonPath("errors[0].arguments.expected").value("String"));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingPartsProperly() throws Exception {
        mvc.perform(multipart("/test/part"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].code").value(MISSING_PART))
            .andExpect(jsonPath("errors[0].message").value("file part is required"))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments.name").value("file"));
    }

    @Test
    public void errorController_ShouldHandleHandleUnauthorizedErrorsProperly() throws Exception {
        mvc.perform(get("/test/protected"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("errors[0].code").value(AUTH_REQUIRED))
            .andExpect(jsonPath("$.fingerprint").exists());
    }

    @Test
    public void errorController_ShouldHandleHandleAccessDeniedErrorsProperly() throws Exception {
        List<GrantedAuthority> authorities = singletonList(new SimpleGrantedAuthority("ROLE_FAKE"));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("me", "pass", authorities);

        mvc.perform(post("/test/protected").with(authentication(authentication)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("errors[0].code").value(ACCESS_DENIED))
            .andExpect(jsonPath("$.fingerprint").exists());
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingHeadersProperly() throws Exception {
        mvc.perform(get("/test/header"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].code").value(MISSING_HEADER))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments.name").value("name"))
            .andExpect(jsonPath("$.errors[0].arguments.expected").value("String"));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingCookiesProperly() throws Exception {
        mvc.perform(get("/test/cookie"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].code").value(MISSING_COOKIE))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments.name").value("name"))
            .andExpect(jsonPath("$.errors[0].arguments.expected").value("String"));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingMatrixVarsProperly() throws Exception {
        mvc.perform(get("/test/matrix"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].code").value(MISSING_MATRIX_VARIABLE))
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[0].arguments.name").value("name"))
            .andExpect(jsonPath("$.errors[0].arguments.expected").value("String"));
    }

    @Test
    public void controllerAdvice_ShouldHandleBindingExceptionsProperly() throws Exception {
        mvc.perform(get("/test/paged").param("page", "nan").param("size", "size").param("sort", "value"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("errors[*].code")
                    .value(containsInAnyOrder(
                        "binding.type_mismatch.page",
                        "binding.type_mismatch.size",
                        "binding.type_mismatch.sort"
                    ))
            )
            .andExpect(jsonPath("$.fingerprint").exists())
            .andExpect(jsonPath("$.errors[*].arguments.property").value(containsInAnyOrder("page", "size", "sort")))
            .andExpect(jsonPath("$.errors[*].arguments.expected").value(containsInAnyOrder("Integer", "Integer", "Sort")))
            .andExpect(jsonPath("$.errors[*].arguments.invalid").value(containsInAnyOrder("nan", "size", "value")));
    }

    @Test
    public void controllerAdvice_ShouldHandleTypeMismatchesAsExpected() throws Exception {
        mvc.perform(get("/test//type-mismatch").param("number", "not a number"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value("binding.type_mismatch.number"))
            .andExpect(jsonPath("$.errors[0].arguments.property").value("number"))
            .andExpect(jsonPath("$.errors[0].arguments.invalid").value("not a number"))
            .andExpect(jsonPath("$.errors[0].arguments.expected").value("Integer"));
    }

    @Test
    public void controllerAdvice_HttpErrorShouldContainAppropriateDetails() throws Exception {
        ArgumentCaptor<HttpError> captor = ArgumentCaptor.forClass(HttpError.class);

        mvc.perform(get("/test//type-mismatch").param("number", "not a number"))
            .andExpect(status().isBadRequest());

        verify(processor).process(captor.capture());
        HttpError value = captor.getValue();
        assertThat(value.getRequest()).isNotNull();
        assertThat(value.getRequest()).isInstanceOf(WebRequest.class);
        assertThat(value.getOriginalException()).isNotNull();
        assertThat(value.getRefinedException()).isNotNull();
    }

    @Test
    public void controllerAdvice_ShouldRejectRequestsWithFileSizeBiggerThanTheMaxThreshold() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, byte[]> data = new LinkedMultiValueMap<>();
        data.add("file", new byte[1025]);

        HttpEntity<MultiValueMap<String, byte[]>> request = new HttpEntity<>(data, headers);

        ResponseEntity<Errors> response = restTemplate.postForEntity("/test/max-size", request, Errors.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors.size()).isOne();
        assertThat(response.getBody().errors.get(0).code).isEqualTo(MultipartWebErrorHandler.MAX_SIZE);
        assertThat(response.getBody().errors.get(0).message).isNull();
    }

    @Test
    public void controllerAdvice_ShouldRejectRequestsWithRequestSizeBiggerThanTheMaxThreshold() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, byte[]> data = new LinkedMultiValueMap<>();
        data.add("file", new byte[1000]);
        data.add("file1", new byte[1000]);
        data.add("file2", new byte[1000]);

        HttpEntity<MultiValueMap<String, byte[]>> request = new HttpEntity<>(data, headers);

        ResponseEntity<Errors> response = restTemplate.postForEntity("/test/max-size", request, Errors.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors.size()).isOne();
        assertThat(response.getBody().errors.get(0).code).isEqualTo(MultipartWebErrorHandler.MAX_SIZE);
        assertThat(response.getBody().errors.get(0).message).isNull();
    }

    @Test
    public void controllerAdvice_ShouldRejectNonMultipartRequestForMultipartEndpoints() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON_UTF8);
        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        ResponseEntity<Errors> response = restTemplate.postForEntity("/test/max-size", request, Errors.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors.size()).isOne();
        assertThat(response.getBody().errors.get(0).code).isEqualTo(MultipartWebErrorHandler.MULTIPART_EXPECTED);
        assertThat(response.getBody().errors.get(0).message).isNull();
    }

    private Object[] provideInvalidBody() {
        return p(
            p(Collections.emptyMap(), null, cm("text.required", "The text is required")),
            p(dto(null, 10, "code"), null, cm("text.required", "The text is required")),
            p(dto("", 10, "code"), null, cm("text.required", "The text is required")),
            p(dto("", 10, "code"), new Locale("fa", "IR"), cm("text.required", "متن اجباری است")),
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

    private HttpError.CodedMessage cm(String code, String message) {
        return new HttpError.CodedMessage(code, message, emptyList());
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    private static class Error {

        private String code;
        private String message;
        private Map<String, String> arguments = new HashMap<>();

        @Override
        public String toString() {
            return "Error{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", arguments=" + arguments +
                '}';
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    private static class Errors {
        private String fingerprint;
        private List<Error> errors = new ArrayList<>();

        @Override
        public String toString() {
            return "Errors{" +
                "fingerprint='" + fingerprint + '\'' +
                ", errors=" + errors +
                '}';
        }
    }
}

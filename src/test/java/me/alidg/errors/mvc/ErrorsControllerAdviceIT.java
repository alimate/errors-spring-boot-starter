package me.alidg.errors.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeAsArg;
import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.handlers.LastResortWebErrorHandler;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.util.ApplicationContextTestUtils;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static me.alidg.Params.p;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static me.alidg.errors.handlers.SpringMvcWebErrorHandler.*;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static me.alidg.errors.mvc.ErrorsControllerAdviceIT.Dto.dto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link ErrorsControllerAdvice}.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ErrorsControllerAdviceIT {

    private final WebApplicationContextRunner simpleContextRunner = new WebApplicationContextRunner();

    private MockMvc mvc;
    private ConfigurableWebApplicationContext context;

    @Before
    public void setUp() {
        String port = "--server.port=0";
        String messages = "--spring.messages.basename=test_messages";
        context = (ConfigurableWebApplicationContext) new SpringApplication(WebConfig.class).run(port, messages);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        ApplicationContextTestUtils.closeAll(context);
    }

    @Test
    public void withoutErrorHandlers_TheControllerAdviceShouldNotGetRegistered() {
        simpleContextRunner.run(ctx ->
                assertThatThrownBy(() -> ctx.getBean(ErrorsControllerAdvice.class))
                        .isInstanceOf(NoSuchBeanDefinitionException.class)
        );
    }

    @Test
    public void withErrorHandlers_TheControllerAdviceShouldBeRegistered() {
        ErrorsControllerAdvice controllerAdvice = context.getBean(ErrorsControllerAdvice.class);

        assertThat(controllerAdvice).isNotNull();
    }

    @Test
    public void controllerAdvice_ShouldBeAbleToHandleUnknownErrors() throws Exception {
        mvc.perform(delete("/test"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors[0].code").value(LastResortWebErrorHandler.UNKNOWN_ERROR_CODE));
    }

    @Test
    public void controllerAdvice_ShouldBeAbleToHandleAnnotatedExceptions() throws Exception {
        mvc.perform(get("/test"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("invalid_params"))
                .andExpect(jsonPath("$.errors[0].message").value("Params are: a, c and 10"));
    }

    @Test
    @Parameters(method = "provideInvalidBody")
    public void controllerAdvice_ShouldHandleValidationErrorsProperly(Object body,
                                                                      Locale locale,
                                                                      CodedMessage... expectedErrors) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(body);

        Object[] codes = Stream.of(expectedErrors).map(CodedMessage::getCode).toArray();
        Object[] messages = Stream.of(expectedErrors).map(CodedMessage::getMessage).toArray();
        mvc.perform(post("/test").locale(locale).contentType("application/json").content(data))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[*].code").value(Matchers.containsInAnyOrder(codes)))
                .andExpect(jsonPath("errors[*].message").value(Matchers.containsInAnyOrder(messages)));

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
                .andExpect(jsonPath("errors[0].code").value(INVALID_OR_MISSING_BODY));
    }

    @Test
    public void controllerAdvice_ShouldHandleInvalidBodiesProperly() throws Exception {
        mvc.perform(post("/test").contentType("application/json").content("gibberish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].code").value(INVALID_OR_MISSING_BODY));
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
                .andExpect(jsonPath("errors[0].message").value("text/gibberish is not supported"));
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
                .andExpect(jsonPath("errors[0].message").value("PUT method is not supported"));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingParametersProperly() throws Exception {
        mvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].code").value(MISSING_PARAMETER))
                .andExpect(jsonPath("errors[0].message").value("Parameter name of type String is required"));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingPartsProperly() throws Exception {
        mvc.perform(multipart("/test/part"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].code").value(MISSING_PART))
                .andExpect(jsonPath("errors[0].message").value("file part is required"));
    }

    @Test
    public void errorController_ShouldHandleHandleUnauthorizedErrorsProperly() throws Exception {
        mvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("errors[0].code").value(AUTH_REQUIRED));
    }

    @Test
    public void errorController_ShouldHandleHandleAccessDeniedErrorsProperly() throws Exception {
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_FAKE"));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("me", "pass", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mvc.perform(post("/test/protected"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("errors[0].code").value(ACCESS_DENIED));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingHeadersProperly() throws Exception {
        mvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].code").value(MISSING_HEADER));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingCookiesProperly() throws Exception {
        mvc.perform(get("/test/cookie"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].code").value(MISSING_COOKIE));
    }

    @Test
    public void controllerAdvice_ShouldHandleMissingMatrixVarsProperly() throws Exception {
        mvc.perform(get("/test/matrix"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].code").value(MISSING_MATRIX_VARIABLE));
    }

    private Object[] provideInvalidBody() {
        return p(
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

    private CodedMessage cm(String code, String message) {
        return new CodedMessage(code, message);
    }

    @EnableWebMvc
    @EnableWebSecurity
    @TestConfiguration
    @Import({
            ErrorsAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            ErrorMvcAutoConfiguration.class,
            ValidationAutoConfiguration.class,
            MessageSourceAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            ServletWebServerFactoryAutoConfiguration.class
    })
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    @ComponentScan(basePackageClasses = ErrorsControllerAdvice.class)
    protected static class WebConfig extends WebSecurityConfigurerAdapter {}

    @RestController
    @RequestMapping("/test")
    protected static class TestController {

        @GetMapping
        public void get() {
            throw new InvalidParamsException(10, "c", "a");
        }

        @PostMapping
        public void post(@RequestBody @Validated Dto dto) {
            System.out.println("DTO received: " + dto);
        }

        @DeleteMapping
        public void delete() {
            throw new IllegalArgumentException();
        }

        @GetMapping("/param")
        public Dto getParam(@RequestParam String name) {
            return new Dto(name, 12, "");
        }

        @PostMapping("/part")
        public MultipartFile postParam(@RequestPart MultipartFile file) {
            return file;
        }

        @GetMapping("/protected")
        @PreAuthorize("isAuthenticated()")
        public void needsAuthentication() {}

        @PostMapping("/protected")
        @PreAuthorize("hasRole('ADMIN')")
        public void needsPermission() {}

        @GetMapping("/header")
        public void headerIsRequired(@RequestHeader String name) {}

        @GetMapping("/cookie")
        public void cookieIsRequired(@CookieValue String name) {}

        @GetMapping("/matrix")
        public void matrixIsRequired(@MatrixVariable String name) {}
    }

    protected static class Dto {

        @NotBlank(message = "{text.required}")
        private String text;

        @Min(value = 0, message = "number.min")
        private int number;

        @Size(min = 1, max = 2, message = "range.limit")
        private List<String> range;

        Dto() {}

        Dto(String text, int number, String... range) {
            this.text = text;
            this.number = number;
            this.range = Arrays.asList(range);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public List<String> getRange() {
            return range;
        }

        public void setRange(List<String> range) {
            this.range = range;
        }

        static Dto dto(String text, int number, String... range) {
            return new Dto(text, number, range);
        }
    }

    @ExceptionMapping(statusCode = HttpStatus.UNPROCESSABLE_ENTITY, errorCode = "invalid_params")
    private static class InvalidParamsException extends RuntimeException {

        @ExposeAsArg(10)
        private final int f;
        @ExposeAsArg(2)
        private final String c;
        @ExposeAsArg(0)
        private final String a;

        InvalidParamsException(int f, String c, String a) {
            this.f = f;
            this.c = c;
            this.a = a;
        }
    }
}

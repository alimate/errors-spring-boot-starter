package me.alidg.errors.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HttpError;
import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.WebErrorHandlerPostProcessor;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeAsArg;
import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.conf.ServletErrorsAutoConfiguration;
import me.alidg.errors.conf.ServletSecurityErrorsAutoConfiguration;
import me.alidg.errors.handlers.LastResortWebErrorHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.util.ApplicationContextTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.request.WebRequest;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.alidg.Params.p;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static me.alidg.errors.mvc.ErrorsControllerAdviceIT.Dto.dto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
        System.setProperty("errors.expose-arguments", "always");
        System.setProperty("errors.add-fingerprint", "true");
        String port = "--server.port=0";
        String messages = "--spring.messages.basename=test_messages";
        context = (ConfigurableWebApplicationContext) new SpringApplication(WebConfig.class)
            .run(port, messages);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @After
    public void tearDown() {
        System.clearProperty("errors.expose-arguments");
        System.clearProperty("errors.add-fingerprint");
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
        CodedMessage... expectedErrors) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(body);

        Object[] codes = Stream.of(expectedErrors).map(CodedMessage::getCode).toArray();
        Object[] messages = Stream.of(expectedErrors).map(CodedMessage::getMessage).toArray();
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
        WebErrorHandlerPostProcessor processor = context.getBean(WebErrorHandlerPostProcessor.class);
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

    private CodedMessage cm(String code, String message) {
        return new CodedMessage(code, message, emptyList());
    }

    protected enum Sort {
        ASC, DESC
    }

    @EnableWebMvc
    @EnableWebSecurity
    @TestConfiguration
    @ImportAutoConfiguration({
        ErrorsAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ErrorMvcAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        ServletErrorsAutoConfiguration.class,
        MessageSourceAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,
        ServletSecurityErrorsAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        ServletWebServerFactoryAutoConfiguration.class
    })
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    @ComponentScan(basePackageClasses = ErrorsControllerAdvice.class)
    protected static class WebConfig extends WebSecurityConfigurerAdapter {

        private final AccessDeniedHandler accessDeniedHandler;
        private final AuthenticationEntryPoint authenticationEntryPoint;

        public WebConfig(
            AccessDeniedHandler accessDeniedHandler,
            AuthenticationEntryPoint authenticationEntryPoint) {
            this.accessDeniedHandler = accessDeniedHandler;
            this.authenticationEntryPoint = authenticationEntryPoint;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .anonymous().disable()
                .csrf().disable()
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint);
        }

        @Bean
        public WebErrorHandlerPostProcessor postProcessor() {
            return mock(WebErrorHandlerPostProcessor.class);
        }
    }

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
        public void needsAuthentication() {
        }

        @PostMapping("/protected")
        @PreAuthorize("hasRole('ADMIN')")
        public void needsPermission() {
        }

        @GetMapping("/header")
        public void headerIsRequired(@RequestHeader String name) {
        }

        @GetMapping("/cookie")
        public void cookieIsRequired(@CookieValue String name) {
        }

        @GetMapping("/matrix")
        public void matrixIsRequired(@MatrixVariable String name) {
        }

        @GetMapping("/type-mismatch")
        public void mismatch(@RequestParam Integer number) {
        }

        @GetMapping("/paged")
        public void pagedResult(Pageable pageable) {
        }
    }

    protected static class Pageable {

        private Integer page;
        private Integer size;
        private Sort sort;

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Sort getSort() {
            return sort;
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }
    }

    protected static class Dto {

        @NotBlank(message = "{text.required}")
        private String text;

        @Min(value = 0, message = "number.min")
        private int number;

        @Size(min = 1, max = 2, message = "range.limit")
        private List<String> range;

        Dto() {
        }

        Dto(String text, int number, String... range) {
            this.text = text;
            this.number = number;
            this.range = Arrays.asList(range);
        }

        static Dto dto(String text, int number, String... range) {
            return new Dto(text, number, range);
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

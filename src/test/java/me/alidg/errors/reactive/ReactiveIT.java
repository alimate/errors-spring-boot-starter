package me.alidg.errors.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.ExceptionLogger;
import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeAsArg;
import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.conf.ReactiveErrorsAutoConfiguration;
import me.alidg.errors.conf.ReactiveSecurityErrorsAutoConfiguration;
import me.alidg.errors.handlers.ServletWebErrorHandler;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.ApplicationContextTestUtils;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static me.alidg.Params.p;
import static me.alidg.errors.handlers.LastResortWebErrorHandler.UNKNOWN_ERROR_CODE;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static me.alidg.errors.reactive.ReactiveIT.Dto.dto;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.*;

/**
 * Integration tests for our reactive stack support.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ReactiveIT {

    private WebTestClient client;
    private ConfigurableReactiveWebApplicationContext context;

    @Before
    public void setUp() {
        System.setProperty("errors.expose-arguments", "non_empty");
        System.setProperty("errors.add-fingerprint", "false");
        String port = "--server.port=0";
        String messages = "--spring.messages.basename=test_messages";
        SpringApplication application = new SpringApplication(WebFluxConfig.class);
        application.setWebApplicationType(WebApplicationType.REACTIVE);

        context = (ConfigurableReactiveWebApplicationContext) application.run(port, messages);

        client = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @After
    public void tearDown() {
        System.clearProperty("errors.expose-arguments");
        System.clearProperty("errors.add-fingerprint");
        ApplicationContextTestUtils.closeAll(context);
    }

    @Test
    public void errorAttributes_ShouldBeAbleToHandleUnknownErrors() {
        client.mutateWith(csrf()).delete().uri("/test").exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                    .jsonPath("$.errors[0].code").isEqualTo(UNKNOWN_ERROR_CODE)
                    .jsonPath("$.fingerprint").doesNotExist()
                    .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger()).log(any());
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

        verify(logger()).log(any(ReactiveIT.InvalidParamsException.class));
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

        verify(logger()).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleHandlerNotFoundSituations() {
        client.get().uri("/should_not_be_found").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                    .jsonPath("errors[0].code").isEqualTo(NO_HANDLER)
                    .jsonPath("$.fingerprint").doesNotExist()
                    .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger()).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingBodiesProperly() {
        client.mutateWith(csrf()).post().uri("/test").contentType(APPLICATION_JSON_UTF8).exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("errors[0].code").isEqualTo(INVALID_OR_MISSING_BODY)
                    .jsonPath("$.fingerprint").doesNotExist()
                    .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger()).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleInvalidBodiesProperly() {
        client.mutateWith(csrf()).post().uri("/test").contentType(APPLICATION_JSON_UTF8).syncBody("gibberish").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("errors[0].code").isEqualTo(INVALID_OR_MISSING_BODY)
                    .jsonPath("$.fingerprint").doesNotExist()
                    .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger()).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleMissingContentTypesProperly() {
        client.mutateWith(csrf()).post().uri("/test").syncBody("gibberish").exchange()
                .expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
                .expectBody()
                    .jsonPath("errors[0].code").isEqualTo(NOT_SUPPORTED)
                    .jsonPath("$.fingerprint").doesNotExist()
                    .jsonPath("$.errors[0].arguments").exists();

        verify(logger()).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleInvalidContentTypesProperly() {
        client.mutateWith(csrf()).post().uri("/test").contentType(new MediaType("text", "gibberish")).syncBody("gibberish").exchange()
                .expectStatus().isEqualTo(UNSUPPORTED_MEDIA_TYPE)
                .expectBody().jsonPath("errors[0].code").isEqualTo(NOT_SUPPORTED);

        verify(logger()).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleInvalidAcceptHeadersProperly(){
        client.get().uri("/test/param?name=ali").accept(IMAGE_JPEG).exchange()
                .expectStatus().isEqualTo(NOT_ACCEPTABLE);

        verify(logger()).log(any());
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

        verify(logger()).log(any());
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

        verify(logger()).log(any());
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

        verify(logger()).log(any());
    }

    @Test
    public void errorAttributes_ShouldHandleHandleUnauthorizedErrorsProperly() {
        client.get().uri("/test/protected").exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                    .jsonPath("errors[0].code").isEqualTo(AUTH_REQUIRED)
                    .jsonPath("$.fingerprint").doesNotExist()
                    .jsonPath("$.errors[0].arguments").doesNotExist();

        verify(logger()).log(any());
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

        verify(logger()).log(any());
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

        verify(logger()).log(any());
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

        verify(logger()).log(any());
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

        verify(logger()).log(any());
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

    @RestController
    @RequestMapping("/test")
    protected static class ReactiveTestController {

        @GetMapping
        public Mono<Void> get() {
            throw new ReactiveIT.InvalidParamsException(10, "c", "a");
        }

        @PostMapping
        public Mono<Void> post(@RequestBody @Validated ReactiveIT.Dto dto) {
            return Mono.empty();
        }

        @DeleteMapping
        public Mono<Void> delete() {
            throw new IllegalArgumentException();
        }

        @GetMapping("/param")
        public Mono<Dto> getParam(@RequestParam String name) {
            return Mono.just(new ReactiveIT.Dto(name, 12, ""));
        }

        @PostMapping(value = "/part", consumes = MULTIPART_FORM_DATA_VALUE)
        public MultipartFile postParam(@RequestPart MultipartFile file) {
            return file;
        }

        @GetMapping("/protected")
        public Mono<Void> needsAuthentication() {
            return Mono.empty();
        }

        @PostMapping("/protected")
        public Mono<Void> needsPermission() {
            return Mono.empty();
        }

        @GetMapping("/header")
        public Mono<Void> headerIsRequired(@RequestHeader String name) {
            return Mono.empty();
        }

        @GetMapping("/cookie")
        public Mono<Void> cookieIsRequired(@CookieValue String name) {
            return Mono.empty();
        }

        @GetMapping("/matrix")
        public Mono<Void> matrixIsRequired(@MatrixVariable String name) {
            return Mono.empty();
        }

        @GetMapping("/paged")
        public void pagedResult(Pageable pageable) {}
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

        static ReactiveIT.Dto dto(String text, int number, String... range) {
            return new ReactiveIT.Dto(text, number, range);
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

    protected enum Sort {
        ASC, DESC
    }

    private MultiValueMap<String, HttpEntity<?>> generateBody() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("fieldPart", "fieldValue");
        builder.part("fileParts", new ByteArrayResource("data".getBytes()));
        return builder.build();
    }

    private ExceptionLogger logger() {
        return context.getBean(ExceptionLogger.class);
    }

    @EnableWebFlux
    @TestConfiguration
    @EnableWebFluxSecurity
    @Import({
            ErrorsAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            WebFluxAutoConfiguration.class,
            ValidationAutoConfiguration.class,
            HttpHandlerAutoConfiguration.class,
            ErrorWebFluxAutoConfiguration.class,
            MessageSourceAutoConfiguration.class,
            ReactiveErrorsAutoConfiguration.class,
            ReactiveSecurityAutoConfiguration.class,
            ReactiveSecurityErrorsAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class,
            ReactiveWebServerFactoryAutoConfiguration.class,
    })
    @EnableReactiveMethodSecurity
    @ComponentScan(basePackageClasses = ReactiveIT.ReactiveTestController.class)
    static class WebFluxConfig extends WebFluxConfigurationSupport {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                             ServerAccessDeniedHandler accessDeniedHandler,
                                                             ServerAuthenticationEntryPoint authenticationEntryPoint) {
            return http
                    .csrf()
                        .accessDeniedHandler(accessDeniedHandler)
                    .and()
                    .exceptionHandling()
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                    .and()
                    .authorizeExchange()
                        .pathMatchers(GET, "/test/protected").authenticated()
                        .pathMatchers(POST, "/test/protected").hasRole("ADMIN")
                        .anyExchange().permitAll()
                    .and().build();
        }

        @Bean
        public ExceptionLogger exceptionLogger() {
            return mock(ExceptionLogger.class);
        }
    }

    @ExceptionMapping(statusCode = UNPROCESSABLE_ENTITY, errorCode = "invalid_params")
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

    private Object[] provideInvalidBody() {
        return p(
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

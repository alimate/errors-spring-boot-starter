package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.validation.*;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static me.alidg.errors.handlers.SpringValidationWebErrorHandler.BINDING_FAILURE;
import static me.alidg.errors.handlers.SpringValidationWebErrorHandlerTest.TBV.tbv;
import static me.alidg.errors.handlers.SpringValidationWebErrorHandlerTest.TBVchild.tbvChild;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpringValidationWebErrorHandler} exception handler.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class SpringValidationWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final SpringValidationWebErrorHandler handler = new SpringValidationWebErrorHandler();

    /**
     * Spring Validator to generate valid {@link BindingResult}s.
     */
    private final Validator validator = new SpringValidatorAdapter(
            Validation.buildDefaultValidatorFactory().getValidator()
    );

    @Test
    @Parameters(method = "provideParamsForCanHandle")
    public void canHandle_ShouldOnlyReturnTrueForSpringSpecificValidationErrors(Throwable exception, boolean expected) {
        assertThat(handler.canHandle(exception))
                .isEqualTo(expected);
    }

    @Test
    @Parameters(method = "provideParamsForHandle")
    public void handle_ShouldHandleTheValidationErrorsProperly(Object toValidate,
                                                               Set<String> errorCodes,
                                                               Map<String, List<Argument>> args) {
        BindingResult result = new BeanPropertyBindingResult(toValidate, "toValidate");
        validator.validate(toValidate, result);

        // Create and assert for BindException

        BindException bindException = new BindException(result);
        HandledException handledForBind = handler.handle(bindException);

        assertThat(handledForBind.getErrorCodes()).containsAll(errorCodes);
        assertThat(handledForBind.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handledForBind.getArguments()).isEqualTo(args);

        // Create and assert for MethodArgumentNotValidException

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, result);
        HandledException handled = handler.handle(exception);

        assertThat(handled.getErrorCodes()).containsAll(errorCodes);
        assertThat(handled.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handled.getArguments()).isEqualTo(args);
    }

    @Test
    public void handle_ForUnknownBindingErrorsShouldReturnBindingFailureErrorCode() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(singletonList(new FieldError("", "", "")));
        BindException exception = new BindException(bindingResult);

        HandledException handledException = handler.handle(exception);
        assertThat(handledException.getArguments()).isEmpty();
        assertThat(handledException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handledException.getErrorCodes()).containsOnly(BINDING_FAILURE);
    }

    private Object[] provideParamsForCanHandle() {
        return p(
                p(null, false),
                p(new RuntimeException(), false),
                p(new BindException(mock(BindingResult.class)), true),
                p(mock(MethodArgumentNotValidException.class), true)
        );
    }

    private Object[] provideParamsForHandle() {
        return p(
                p(tbv("ali", 0, "coding"), e("age.min"),
                        singletonMap("age.min", asList(
                                arg("value", 1L),
                                arg("invalid", 0),
                                arg("property", "age")))),
                p(tbv("ali", 29), e("interests.limit"),
                        singletonMap("interests.limit", asList(
                                arg("max", 6),
                                arg("min", 1),
                                arg("invalid", emptyList()),
                                arg("property", "interests")))),
                p(tbv("", 29, "coding"), e("name.required"),
                        singletonMap("name.required", asList(
                                arg("invalid", ""),
                                arg("property", "name")))),
                p(tbv("", 200), e("name.required", "age.max", "interests.limit"),
                        m(
                                "age.max", asList(
                                        arg("value", 100L),
                                        arg("invalid", 200),
                                        arg("property", "age")),
                                "interests.limit", asList(
                                        arg("max", 6),
                                        arg("min", 1),
                                        arg("invalid", emptyList()),
                                        arg("property", "interests")),
                                "name.required", asList(
                                        arg("invalid", ""),
                                        arg("property", "name"))
                        )
                ),
                p(tbv("ali", 29, singletonList("coding"), asList(tbvChild("given"), tbvChild(""), tbvChild("also given"))),
                        e("stringField.required"),
                        singletonMap("stringField.required", asList(
                                arg("invalid", ""),
                                arg("property", "tbvChildren[1].stringField"))))
        );
    }

    private Set<String> e(String... errorCodes) {
        return new HashSet<>(asList(errorCodes));
    }

    private Map<String, Object> m(String k1, List<Object> v1, String k2, List<Object> v2, String k3, List<Object> v3) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /**
     * A To Be Validated (TBV) class!
     */
    static class TBV {

        @NotBlank(message = "{name.required}")
        private String name;

        @Min(value = 1, message = "age.min")
        @Max(value = 100, message = "age.max")
        private int age;

        @Size(min = 1, max = 6, message = "interests.limit")
        private List<String> interests;

        @Valid
        private List<TBVchild> tbvChildren;

        TBV(String name, int age, List<String> interests) {
            this.name = name;
            this.age = age;
            this.interests = interests;
        }

        TBV(String name, int age, List<String> interests, List<TBVchild> tbvChildren) {
            this.name = name;
            this.age = age;
            this.interests = interests;
            this.tbvChildren = tbvChildren;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public List<String> getInterests() {
            return interests;
        }

        public void setInterests(List<String> interests) {
            this.interests = interests;
        }

        public List<TBVchild> getTbvChildren() {
            return tbvChildren;
        }

        public void setTbvChildren(List<TBVchild> tbvChildren) {
            this.tbvChildren = tbvChildren;
        }

        static TBV tbv(String name, int age, String... interests) {
            return new TBV(name, age, asList(interests));
        }
        static TBV tbv(String name, int age, List<String> interests, List<TBVchild> tbvChildren) {
            return new TBV(name, age, interests, tbvChildren);
        }
    }

    static class TBVchild {
        @NotBlank(message = "stringField.required")
        private String stringField;

        TBVchild(String stringField) {
            this.stringField = stringField;
        }

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }

        public static TBVchild tbvChild(String stringField) {
            return new TBVchild(stringField);
        }
    }
}

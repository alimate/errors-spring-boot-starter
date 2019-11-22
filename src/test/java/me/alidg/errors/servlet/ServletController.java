package me.alidg.errors.servlet;

import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeAsArg;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/test")
public class ServletController {

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

    @PostMapping("/max-size")
    public void upload(@RequestPart MultipartFile file) {}

    @PostMapping("/default-codes")
    public void defaultCodes(@RequestBody @Valid DefaultDto dto) {}

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

    protected static class DefaultDto {

        @Email
        private String userEmail = "valid@v.com";

        @AssertTrue
        private boolean isActive = true;

        @AssertFalse
        private boolean isFailed = false;

        @DecimalMax(value = "12")
        private BigDecimal decimalMax = BigDecimal.valueOf(12);

        @DecimalMin(value = "10")
        private BigDecimal decimalMin = BigDecimal.valueOf(15);

        @Valid
        private Wrapper wrapper = new Wrapper().setName("default");

        public String getUserEmail() {
            return userEmail;
        }

        public DefaultDto setUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public boolean isActive() {
            return isActive;
        }

        public DefaultDto setActive(boolean active) {
            isActive = active;
            return this;
        }

        public boolean isFailed() {
            return isFailed;
        }

        public DefaultDto setFailed(boolean failed) {
            isFailed = failed;
            return this;
        }

        public BigDecimal getDecimalMax() {
            return decimalMax;
        }

        public DefaultDto setDecimalMax(BigDecimal decimalMax) {
            this.decimalMax = decimalMax;
            return this;
        }

        public BigDecimal getDecimalMin() {
            return decimalMin;
        }

        public DefaultDto setDecimalMin(BigDecimal decimalMin) {
            this.decimalMin = decimalMin;
            return this;
        }

        public Wrapper getWrapper() {
            return wrapper;
        }

        public DefaultDto setWrapper(Wrapper wrapper) {
            this.wrapper = wrapper;
            return this;
        }
    }

    protected static class Wrapper {

        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public Wrapper setName(String name) {
            this.name = name;
            return this;
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

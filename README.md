<p align="center">
    <img width="300" height="300" src="https://imgur.com/cOcB3kB.png" />
</p>

<h1 align="center">Errors Spring Boot Starter</h1> 

[![Build Status](https://travis-ci.org/alimate/errors-spring-boot-starter.svg?branch=master)](https://travis-ci.org/alimate/errors-spring-boot-starter) 
[![codecov](https://codecov.io/gh/alimate/errors-spring-boot-starter/branch/master/graph/badge.svg)](https://codecov.io/gh/alimate/errors-spring-boot-starter) 
[![Maven Central](https://img.shields.io/maven-central/v/me.alidg/errors-spring-boot-starter.svg)](https://search.maven.org/search?q=g:me.alidg%20AND%20a:errors-spring-boot-starter) 
[![Javadocs](http://www.javadoc.io/badge/me.alidg/errors-spring-boot-starter.svg)](http://www.javadoc.io/doc/me.alidg/errors-spring-boot-starter) 
[![Sonatype](https://img.shields.io/static/v1.svg?label=snapshot&message=v1.5.0-SNAPSHOT&color=blueviolet)](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=me.alidg&a=errors-spring-boot-starter&v=1.5.0-SNAPSHOT&e=jar) 
[![Sonar Quality Gate](https://img.shields.io/sonar/quality_gate/alimate_errors-spring-boot-starter?label=code%20quality&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=alimate_errors-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<p align="center"><b>A Bootiful, Consistent and Opinionated Approach to Handle all sorts of Exceptions.</b></p>

## Table of Contents

  * [Make Error Handling Great Again!](#make-error-handling-great-again)
  * [Getting Started](#getting-started)
    + [Download](#download)
    + [Prerequisites](#prerequisites)
    + [Overview](#overview)
    + [Error Codes](#error-codes)
    + [Error Message](#error-message)
    + [Exposing Arguments](#exposing-arguments)
      + [Exposing Named Arguments](#exposing-named-arguments)
      + [Named Arguments Interpolation](#named-arguments-interpolation)
    + [Validation and Binding Errors](#validation-and-binding-errors)
    + [Custom Exceptions](#custom-exceptions)
    + [Spring MVC](#spring-mvc)
    + [Spring Security](#spring-security)
      + [Reactive](#reactive-security)
      + [Servlet](#servlet-security)
    + [Error Representation](#error-representation)
      + [Fingerprinting](#fingerprinting)
      + [Customizing the Error Representation](#customizing-the-error-representation)
    + [Default Error Handler](#default-error-handler)
    + [Refining Exceptions](#refining-exceptions)
    + [Logging Exceptions](#logging-exceptions)
    + [Post Processing Handled Exceptions](#post-processing-handled-exceptions)
    + [Registering Custom Handlers](#registering-custom-handlers)
    + [Test Support](#test-support)
  * [Appendix](#appendix)
    + [Configuration](#configuration)
  * [License](#license)

## Make Error Handling Great Again!
Built on top of Spring Boot's great exception handling mechanism, the `errors-spring-boot-starter` offers:
 - A consistent approach to handle all exceptions. Doesn't matter if it's a validation/binding error or a 
 custom domain-specific error or even a Spring related error, All of them would be handled by a `WebErrorHandler`
 implementation (No more `ErrorController` vs `@ExceptionHandler` vs `WebExceptionHandler`)
 - Built-in support for application specific error codes, again, for all possible errors.
 - Simple error message interpolation using plain old `MessageSource`s.
 - Customizable HTTP error representation.
 - Exposing arguments from exceptions to error messages.
 - Supporting both traditional and reactive stacks.
 - Customizable exception logging.
 - Supporting error fingerprinting.

## Getting Started

### Download

Download the [latest JAR](https://search.maven.org/remotecontent?filepath=me/alidg/errors-spring-boot-starter/1.4.0/errors-spring-boot-starter-1.4.0.jar) or grab via Maven:

```xml
<dependency>
    <groupId>me.alidg</groupId>
    <artifactId>errors-spring-boot-starter</artifactId>
    <version>1.4.0</version>
</dependency>
```

or Gradle:
```
compile "me.alidg:errors-spring-boot-starter:1.4.0"
```

If you like to stay at the cutting edge, use our `1.5.0-SNAPSHOT` version. Of course you should define the following 
snapshot repository:
```xml
<repositories>
    <repository>
        <id>Sonatype</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
</repositories>
```
or:
```groovy
repositories {
    maven {
      url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
}
```

### Prerequisites
The main dependency is JDK 8+. Tested with:
 - JDK 8, JDK 9, JDK 10 and JDK 11 on Linux.
 - Spring Boot `2.2.0.RELEASE` (Also, should work with any `2.0.0+`)

### Overview
The `WebErrorHandler` implementations are responsible for handling different kinds of exceptions. When an exception 
happens, the `WebErrorHandlers` (A factory over all `WebErrorHandler` implementations) catches the exception and would 
find an appropriate implementation to handle the exception. By default, `WebErrorHandlers` consults with the
following implementations to handle a particular exception:
 - An implementation to handle all validation/binding exceptions.
 - An implementation to handle custom exceptions annotated with the `@ExceptionMapping`.
 - An implementation to handle Spring MVC specific exceptions.
 - And if the Spring Security is on the classpath, An implementation to handle Spring Security specific exceptions.

After delegating to the appropriate handler, the `WebErrorHandlers` turns the handled exception result into a `HttpError`,
which encapsulates the HTTP status code and all error code/message combinations.

### Error Codes
Although using appropriate HTTP status codes is a recommended approach in RESTful APIs, sometimes, we need more information 
to find out what exactly went wrong. This is where *Error Codes* comes in. You can think of an error code as a *Machine Readable* 
description of the error. Each exception can be mapped to **at least one** error code.

In `errors-spring-boot-starter`, one can map exceptions to error codes in different ways:
 - Validation error codes can be extracted from the *Bean Validation*'s constraints:
     ```java
     public class User {  
    
         @NotBlank(message = "username.required")
         private final String username;
      
         @NotBlank(message = "password.required")
         @Size(min = 6, message = "password.min_length")
         private final String password;
      
         // constructor and getter and setters
     }
     ```
    To report a violation in password length, the `password.min_length` would be reported as the error code. As you may guess,
    one validation exception can contain multiple error codes to report all validation violations at once.
    
 - Specifying the error code for custom exceptions using the `@ExceptionMapping` annotation:
    ```java
    @ExceptionMapping(statusCode = BAD_REQUEST, errorCode = "user.already_exists")
    public class UserAlreadyExistsException extends RuntimeException {}
    ```
    The `UserAlreadyExistsException` exception would be mapped to `user.already_exists` error code.
 
 - Specifying the error code in a `WebErrorHandler` implementation:
    ```java
    public class ExistedUserHandler implements WebErrorHandler {
    
        @Override
        public boolean canHandle(Throwable exception) {
            return exception instanceof UserAlreadyExistsException;
        }
     
        @Override
        public HandledException handle(Throwable exception) {   
            return new HandledException("user.already_exists", BAD_REQUEST, null);
        }
    }
    ```

### Error Message
Once the exception mapped to error code(s), we can add a companion and *Human Readable* error message. This can be done
by registering a Spring `MessageSource` to perform the *code-to-message* translation. For example, if we add the following
key-value pair in our message resource file:
```properties
user.already_exists=Another user with the same username already exists
```
Then if an exception of type `UserAlreadyExistsException` was thrown, you would see a `400 Bad Request` HTTP response 
with a body like:
```json
{
  "errors": [
    {
      "code": "user.already_exists",
      "message": "Another user with the same username already exists"
    }
  ]
}
```
Since `MessageSource` supports Internationalization (i18n), our error messages can possibly have different values based
on each *Locale*.

### Exposing Arguments
With *Bean Validation* you can pass parameters from the constraint validation, e.g. `@Size`, to its corresponding 
interpolated message. For example, if we have:
```properties
password.min_length=The password must be at least {0} characters
```
And a configuration like:
```java
@Size(min = 6, message = "password.min_length")
private final String password;
```
The `min` attribute from the `@Size` constraint would be passed to the message interpolation mechanism, so:
```json
{
  "errors": [
    {
      "code": "password.min_length",
      "message": "The password must be at least 6 characters"
    }
  ]
}
```
In addition to support this feature for validation errors, we extend it for custom exceptions using the `@ExposeArg`
annotation. For example, if we're going to specify the already taken username in the message:
```properties
user.already_exists=Another user with name '{username}' and e-mail '{email}' already exists
```
We could write:
```java
@ExceptionMapping(statusCode = BAD_REQUEST, errorCode = "user.already_exists")
public class UserAlreadyExistsException extends RuntimeException {
    @ExposeArg private final String username;
    @ExposeArg private final String email;
    
    // constructor
}
```
Then the `username` property from the `UserAlreadyExistsException` would be available to the message under the 
`user.already_exists` key as the `{username}` argument. `@ExposeArg` can be used on fields and no-arg methods with a
return type. The `HandledException` class also accepts the *to-be-exposed* arguments in its constructor.

Instead of using argument names, argument position can be used instead. Exposed arguments are ordered with respect to:
 1. `order` attribute of `@ExposeArg`, then
 2. `value` attribute of `@ExposeArg` (which is optional name for exposed argument), then
 3. name of annotated element. 

#### Exposing Named Arguments
By default error arguments will be used in message interpolation only. It is also possible to additionally get those
arguments in error response by defining the configuration property `errors.expose-arguments`.
When enabled, you might get the following response payload:
```json
{
  "errors": [
    {
      "code": "password.min_length",
      "message": "The password must be at least 6 characters",
      "arguments": {
        "min": 6
      }
    }
  ]
}
```

The `errors.expose-arguments` property takes 3 possible values:
 - `NEVER` - named arguments will never be exposed. This is the default setting.
 - `NON_EMPTY` - named arguments will be exposed only in case there are any. If error has no arguments,
   result payload will not have `"arguments"` element.
 - `ALWAYS` - the `"arguments"` element is always present in payload, even when the error has no arguments.
   In that case empty map will be provided: `"arguments": {}`.
   
Checkout [here](EXPOSED-ARGS.md) for more detail on how we expose arguments for different exception categories.

#### Named Arguments Interpolation
You can use either positional or named argument placeholders in message templates. Given:
```java
@Size(min = 6, max = 20, message = "password.length")
private final String password;
```
You can create message template in `messages.properties` with positional arguments:
```properties
password.length=Password must have length between {1} and {0}
```
Arguments are sorted by name. Since lexicographically `max` < `min`, placeholder `{0}` will be substituted
with argument `max`, and `{1}` will have value of argument `min`.

You can also use argument names as placeholders:
```properties
password.length=Password must have length between {min} and {max}
```
Named arguments interpolation works out of the box, regardless of the `errors.expose-arguments` value.
You can mix both approaches, but it is not recommended.

If there is a value in the message that *should not* be interpolated, escape the first `{` character with a backslash:

```properties
password.length=Password \\{min} is {min} and \\{max} is {max}
```
After interpolation, this message would read: `Password {min} is 6 and {max} is 20`.

Arguments annotated with `@ExposeArg` will be named by annotated field or method name:
```java
@ExposeArg
private final String argName; // will be exposed as "argName"
```
This can be changed by the `value` parameter:
```java
@ExposeArg("customName")
private final String argName; // will be exposed as "customName"
```

### Validation and Binding Errors
Validation errors can be processed as you might expect. For example, if a client passed an empty JSON to a controller method
like:
```java
@PostMapping
public void createUser(@RequestBody @Valid User user) {
    // omitted
}
```
Then the following error would be returned:
```json
{
  "errors": [
    {
      "code": "password.min_length",
      "message": "corresponding message!"
    },
    {
       "code": "password.required",
       "message": "corresponding message!"
    },
    {
      "code": "username.required",
      "message": "corresponding message!"
    }
  ]
}
```
Bean Validation's `ConstraintViolationException`s will be handled in the same way, too.

### Custom Exceptions
Custom exceptions can be mapped to status code and error code combination using the `@ExceptionMapping` annotation:
```java
@ExceptionMapping(statusCode = BAD_REQUEST, errorCode = "user.already_exists")
public class UserAlreadyExistsException extends RuntimeException {}
```
Here, every time we catch an instance of `UserAlreadyExistsException`, a `Bad Request` HTTP response with `user.already_exists`
error would be returned.

Also, it's possible to expose some arguments from custom exceptions to error messages using the `ExposeArg`:
```java
@ExceptionMapping(statusCode = BAD_REQUEST, errorCode = "user.already_exists")
public class UserAlreadyExistsException extends RuntimeException {
    @ExposeArg(order = 0) private final String username;
    
    // constructor
    
    @ExposeArg(order = 1)
    public String exposeThisToo() {
        return "42";
    }
}
```
Then the error message template can be something like:
```properties
user.already_exists=Another user exists with the '{0}' username: {1}
```
During message interpolation, the `{0}` and `{1}` placeholders would be replaced with annotated field's value and
method's return value. The `ExposeArg` annotation is applicable to:
 - Fields
 - No-arg methods with a return type

### Spring MVC
By default, a custom `WebErrorHandler` is registered to handle common exceptions thrown by Spring MVC:

|                 Exception                 | Status Code |           Error Code          |               Exposed Args               |
|:-----------------------------------------:|:-----------:|:-----------------------------:|:----------------------------------------:|
|     `HttpMessageNotReadableException`     |     400     | `web.invalid_or_missing_body` |                     -                    |
|   `HttpMediaTypeNotAcceptableException`   |     406     |      `web.not_acceptable`     |       List of acceptable MIME types      |
|    `HttpMediaTypeNotSupportedException`   |     415     |  `web.unsupported_media_type` |       The unsupported content type       |
|  `HttpRequestMethodNotSupportedException` |     405     |    `web.method_not_allowed`   |          The invalid HTTP method         |
| `MissingServletRequestParameterException` |     400     |    `web.missing_parameter`    | Name and type of the missing query Param |
|    `MissingServletRequestPartException`   |     400     |       `web.missing_part`      |         Missing request part name        |
|         `NoHandlerFoundException`         |     404     |        `web.no_handler`       |             The request path             |
|      `MissingRequestHeaderException`      |     400     |      `web.missing_header`     |          The missing header name         |
|      `MissingRequestCookieException`      |     400     |      `web.missing_cookie`     |          The missing cookie name         |
|      `MissingMatrixVariableException`     |     400     | `web.missing_matrix_variable` |     The missing matrix variable name     |
|                  `others`                 |     500     |        `unknown_error`        |                     -                    |

Also, almost all exceptions from the `ResponseStatusException` hierarchy, added in Spring Framework 5+ , are handled compatible
with the Spring MVC traditional exceptions.

### Spring Security
When Spring Security is present on the classpath, a `WebErrorHandler` implementation would be responsible to handle
common Spring Security exceptions:

|                   Exception                  | Status Code |         Error Code         |
|:--------------------------------------------:|:-----------:|:--------------------------:|
| `AccessDeniedException`                      |     403     | `security.access_denied`   |
| `AccountExpiredException`                    |     400     | `security.account_expired` |
| `AuthenticationCredentialsNotFoundException` |     401     | `security.auth_required`   |
| `AuthenticationServiceException`             |     500     | `security.internal_error`  |
| `BadCredentialsException`                    |     400     | `security.bad_credentials` |
| `UsernameNotFoundException`                  |     400     | `security.bad_credentials` |
| `InsufficientAuthenticationException`        |     401     | `security.auth_required`   |
| `LockedException`                            |     400     | `security.user_locked`     |
| `DisabledException`                          |     400     | `security.user_disabled`   |
| `others`                                     |     500     | `unknown_error`            |

#### Reactive Security
When the Spring Security is detected along with the Reactive stack, the starter registers two extra handlers to handle
all security related exceptions. In contrast with other handlers which register themselves automatically, in order to use these
two handlers, you should register them in your security configuration manually as follows:
```java
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

    // other configurations

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            ServerAccessDeniedHandler accessDeniedHandler,
                                                            ServerAuthenticationEntryPoint authenticationEntryPoint) {
        http
                .csrf().accessDeniedHandler(accessDeniedHandler)
                .and()
                .exceptionHandling()
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint)
                // other configurations

        return http.build();
    }
}
```
The registered `ServerAccessDeniedHandler` and `ServerAuthenticationEntryPoint` are responsible for handling `AccessDeniedException`
and `AuthenticationException` exceptions, respectively.

#### Servlet Security
When the Spring Security is detected along with the traditional servlet stack, the starter registers two extra handlers to handle
all security related exceptions. In contrast with other handlers which register themselves automatically, in order to use these
two handlers, you should register them in your security configuration manually as follows:
```java
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(AccessDeniedHandler accessDeniedHandler, AuthenticationEntryPoint authenticationEntryPoint) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .exceptionHandling()
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint);
    }
}
```
The registered `AccessDeniedHandler` and `AuthenticationEntryPoint` are responsible for handling `AccessDeniedException`
and `AuthenticationException` exceptions, respectively.

### Error Representation
By default, errors would manifest themselves in the HTTP response bodies with the following JSON schema:
```json
{
  "errors": [
    {
      "code": "the_error_code",
      "message": "the_error_message"
    }
  ]
}
```

#### Fingerprinting
There is also an option to generate error `fingerprint`. Fingerprint is a unique hash of error
event which might be used as a correlation ID of error presented to user, and reported in
application backend (e.g. in detailed log message). To generate error fingerprints, add
the configuration property `errors.add-fingerprint=true`.

We provide two fingerprint providers implementations:
 - `UuidFingerprintProvider` which generates a random UUID regardless of the handled exception.
   This is the default provider and will be used out of the box if
   `errors.add-fingerprint=true` property is configured.
 - `Md5FingerprintProvider` which generates MD5 checksum of full class name of original exception
   and current time.

#### Customizing the Error Representation
In order to change the default error representation, just implement the `HttpErrorAttributesAdapter` 
interface and register it as *Spring Bean*:
```java
@Component
public class OopsDrivenHttpErrorAttributesAdapter implements HttpErrorAttributesAdapter {
    
    @Override
    public Map<String, Object> adapt(HttpError httpError) {
        return Collections.singletonMap("Oops!", httpError.getErrors());
    }
}
```

### Default Error Handler
By default, when all registered `WebErrorHandler`s refuse to handle a particular exception, the `LastResortWebErrorHandler`
would catch the exception and return a `500 Internal Server Error` with `unknown_error` as the error code.

If you don't like this behavior, you can change it by registering a *Bean* of type `WebErrorHandler` with
the `defaultWebErrorHandler` as the *Bean Name*:

```java
@Component("defaultWebErrorHandler")
public class CustomDefaultWebErrorHandler implements WebErrorHandler {
    // Omitted
}
```

### Refining Exceptions
Sometimes the given exception is not the actual problem and we need to dig deeper to handle the error, say the actual
exception is hidden as a cause inside the top-level exception. In order to transform some exceptions before handling 
them, we can register an `ExceptionRefiner` implementation as a *Spring Bean*:
```java
@Component
public class CustomExceptionRefiner implements ExceptionRefiner {
    
    @Override
    Throwable refine(Throwable exception) {
        return exception instanceof ConversionFailedException ? exception.getCause() : exception;
    }
}
```

### Logging Exceptions
By default, the starter issues a few `debug` logs under the `me.alidg.errors.WebErrorHandlers` logger name.
In order to customize the way we log exceptions, we just need to implement the `ExceptionLogger` interface and register it
as a *Spring Bean*:
```java
@Component
public class StdErrExceptionLogger implements ExceptionLogger {
    
    @Override
    public void log(Throwable exception) {
        if (exception != null)
            System.err.println("Failed to process the request: " + exception);
    }
}
``` 

### Post Processing Handled Exceptions
As a more powerful alternative to `ExceptionLogger` mechanism, there is also `WebErrorHandlerPostProcessor`
interface. You may declare multiple post processors which implement this interface and are exposed
as *Spring Bean*. Below is an example of more advanced logging post processors:
```java
@Component
public class LoggingErrorWebErrorHandlerPostProcessor implements WebErrorHandlerPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(LoggingErrorWebErrorHandlerPostProcessor.class);
    
    @Override 
    public void process(@NonNull HttpError error) {
        if (error.getHttpStatus().is4xxClientError()) {
            log.warn("[{}] {}", error.getFingerprint(), prepareMessage(error));
        } else if (error.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}", error.getFingerprint(), prepareMessage(error), error.getOriginalException());
        }
    }
    
    private String prepareMessage(HttpError error) {
        return error.getErrors().stream()
                    .map(HttpError.CodedMessage::getMessage)
                    .collect(Collectors.joining("; "));
    }
}
```

### Registering Custom Handlers
In order to provide a custom handler for a specific exception, just implement the `WebErrorHandler` interface for that
exception and register it as a *Spring Bean*:
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomWebErrorHandler implements WebErrorHandler {
    
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ConversionFailedException;
    }

    @Override
    public HandledException handle(Throwable exception) {
        return new HandledException("custom_error_code", HttpStatus.BAD_REQUEST, null);
    }
}

```
If you're going to register multiple handlers, you can change their priority using `@Order`. Please note that all your custom
handlers would be registered after built-in exception handlers (Validation, `ExceptionMapping`, etc.). If you don't like
this idea, provide a custom *Bean* of type `WebErrorHandlers` and the default one would be discarded.

### Test Support
In order to enable our test support for `WebMvcTest`s, just add the `@AutoConfigureErrors` annotation to your test
class. That's how a `WebMvcTest` would look like with errors support enabled:
```java
@AutoConfigureErrors
@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
public class UserControllerIT {
    
    @Autowired private MockMvc mvc;
    
    @Test
    public void createUser_ShouldReturnBadRequestForInvalidBodies() throws Exception {
        mvc.perform(post("/users").content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value("username.required"));    
    }
}
```
For `WebFluxTest`s, the test support is almost the same as the Servlet stack:
```java
@AutoConfigureErrors
@RunWith(SpringRunner.class)
@WebFluxTest(UserController.class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration.class) // Drop this if you're using Spring Boot 2.1.4+
public class UserControllerIT {

    @Autowired private WebTestClient client;

    @Test
    public void createUser_ShouldReturnBadRequestForInvalidBodies() {
        client.post()
                .uri("/users")
                .syncBody("{}").header(CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors[0].code").isEqualTo("username.required");
    }
}
```

## Appendix

### Configuration
Additional configuration of this starter can be provided by configuration properties - the Spring Boot way.
All configuration properties start with `errors`. Below is a list of supported properties:

|         Property          |             Values             | Default value |
|:-------------------------:|:------------------------------:|:-------------:|
| `errors.expose-arguments` | `NEVER`, `NON_EMPTY`, `ALWAYS` |    `NEVER`    |
| `errors.add-fingerprint`  |        `true`, `false`         |    `false`    |

Check `ErrorsProperties` implementation for more details.

## License
Copyright 2018 alimate

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

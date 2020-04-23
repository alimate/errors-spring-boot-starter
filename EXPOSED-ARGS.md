Exposed Arguments
--
The starter can interpolate both named arguments and positional arguments. Hence, Each exposed argument can be accessed via
both types, too. For example, we may access an argument named `min` by `{min}` or `{0}` placeholders. Although it's possible
to mix and match those two types, we highly discourage this behavior.

## Table of Contents

  * [Validation Exceptions](#validation-exceptions) 
  * [Web Exceptions](#web-exceptions)
  * [Annotated Exceptions](#annotated-exceptions)
  
## Validation Exceptions
For any violation of a Bean Validation constraint, e.g. `@NotBlank`, we expose two sets of arguments:
 1. All annotation attributes will be exposed except for `groups`, `message` and `payload` attributes. We expose those 
 attributes under their attribute name, e.g. `min` for `min` attribute in `@Size`. Also, the positional index of such 
 attributes will be determined lexicographically. For example, `max` and `min` attributes will be exposed on `0` and `1`
 indices because the `max` comes before the `min` lexicographically.
 2. After exposing annotation attributes, we expose two more attributes:
    - `invalid` encapsulates the passed invalid value.
    - `property` represents the property path for the violated constraints.
  
Consider the following POJO:
```java
public class NewUserDto {
    
    @NotBlank(message = "username.required")
    @Size(min = 6, max = 30, message = "username.size")
    private String username;
    
    @Min(18)
    private Integer age;
    
    // getters and setters
}
```

If we pass a value like `ali` as the `username`, then we violate the constraint enforced by `@Size`, so the following
arguments will be exposed:

| Description   | Named Argument | Positional Index |    Value   |
|:-------------:|:--------------:|:----------------:|:----------:|
| Maximum       | `max`          | 0                | `30`       |
| Minimum       | `min`          | 1                | `6`        |
| Invalid Value | `invalid`      | 2                | `ali`      |
| Property Path | `property`     | 3                | `username` |

If we pass an invalid value for the `age` parameter, say `17`, here are the exposed args:

| Description   | Named Argument | Positional Index |    Value   |
|:-------------:|:--------------:|:----------------:|:----------:|
| Minimum       | `value`        | 0                | `18`       |
| Invalid Value | `invalid`      | 1                | `17`       |
| Property Path | `property`     | 2                | `age`      |

And for very simple annotations like `@NotBlank`:

| Description   | Named Argument | Positional Index |    Value   |
|:-------------:|:--------------:|:----------------:|:----------:|
| Invalid Value | `invalid`      | 0                | `  `       |
| Property Path | `property`     | 1                | `username` |

## Web Exceptions
We've tried our best to handle web related exceptions consistently across both Servlet and Reactive stack.

#### Missing Request Params, Headers and Cookies
For any missing header, request param or cookie param, the starter exposes the following:

| Description   | Named Argument | Positional Index |    Value   |
|:-------------:|:--------------:|:----------------:|:----------:|
| Parameter Name| `name`         | 0                | `name`     |
| Expected Type | `expected`     | 1                | `String`   |

Basically:
 - `name` represents the name of the required parameter (`@RequestHeader`, `@RequestParam`, and `@CookieValue`)
 - `expected` represents the simple type name of the required parameter, e.g. `String` for `java.lang.String`.

#### Missing File Parts
Missing file parts, i.e. `@RequestPart`s, only exposes an argument named `name` representing the named of the required
file part parameter.

#### Not Found Exceptions
For not found exceptions the only exposed argument is the `path`.

#### Method Not Allowed
The invalid `method` name is the only exposed argument for `Method Not Allowed`s.

#### Not Acceptable
The exposed `types` argument would contain a list of acceptable media types, something like 
`["application/xml", "application/json"]`.

#### Unsupported Media Types (Servlet)
For `Unsupported Media Type`s in the Servlet stack, we only expose the invalid media type as the `type` argument.

#### Unsupported Media Types (Reactive)
For `Unsupported Media Type`s in the Reactive stack, we only expose all supported media types as the `types` argument.

## Annotated Exceptions
For exceptions annotated with our `@ExceptionMapping` annotation, we only expose fields or no-arg methods annotated with the
`@ExposeArg` annotation. The order of positional arguments is determined by:
 1. the `order` attribute of the `@ExposeArg`, then
 2. the `value` attribute of the `@ExposeArg`, then
 3. the name of annotated element.
Named argument is the field or method name unless we provide a non-blank value using the `name` attribute.

For example, for the following exception:
```java
@ExceptionMapping(errorCode = "code", statusCode = HttpStatus.BAD_REQUEST)
public class SomeException extends RuntimeException {
    
    @ExposeArg(order = -1)
    private final String name;
    
    @ExposeArg(value = "num1", order = 10)
    private final Integer number;
    
    @ExposeArg(value = "num2", order = 10)
    private final Integer anotherNumber;
    
    @ExposeArg
    private final String lastArgument;
    
    // constructor
    
    @ExposeArg(order = 3)
    public String getSomething() {
        // implementation
    }
    
    @ExposeArg(order = 6, value = "another")
    public long getAnotherThing() {
        // implementation
    }
}
```

We expose:

| Named Argument | Positional Index |
|:--------------:|:----------------:|
| `name`         | 0                |
| `getSomething` | 1                |
| `another`      | 2                |
| `num1`         | 3                |
| `num2`         | 4                |
| `lastArgument` | 5                |

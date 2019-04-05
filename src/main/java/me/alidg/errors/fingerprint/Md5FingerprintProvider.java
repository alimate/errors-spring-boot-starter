package me.alidg.errors.fingerprint;

import me.alidg.errors.FingerprintProvider;
import me.alidg.errors.HttpError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A MD5 based implementation of {@link FingerprintProvider} which generates a
 * fingerprint from the handled exception using the following formula:
 * <pre>
 *     md5(exceptionName + currentTimeInMillis)
 * </pre>
 *
 * @author zarebski-m
 */
public class Md5FingerprintProvider implements FingerprintProvider {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(Md5FingerprintProvider.class);

    /**
     * Generates a fingerprint based on the exception and the current timestamp.
     *
     * @param httpError Error event for which fingerprint is generated.
     * @return The generated fingerprint.
     */
    @Override
    public String generate(@NonNull HttpError httpError) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            write(outputStream, exceptionName(httpError));
            write(outputStream, System.currentTimeMillis());

            // To ensure that the fingerprint is unique when the same exception is handled
            // at almost the same time in the same JVM instance.
            write(outputStream, System.nanoTime());

            return DigestUtils.md5DigestAsHex(outputStream.toByteArray());
        } catch (Exception e) {
            logger.warn("Failed to generate a fingerprint for {}", httpError);
            return null;
        }
    }

    private String exceptionName(HttpError httpError) {
        return Optional.ofNullable(httpError.getOriginalException())
            .map(Throwable::getClass)
            .map(Class::getName)
            .orElse("no-exception");
    }

    private void write(OutputStream os, String toWrite) {
        try {
            os.write(toWrite.getBytes(UTF_8));
        } catch (IOException ignored) {
        }
    }

    private void write(OutputStream os, long timestamp) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(0, timestamp);
            os.write(buffer.array());
        } catch (IOException ignored) {
        }
    }
}

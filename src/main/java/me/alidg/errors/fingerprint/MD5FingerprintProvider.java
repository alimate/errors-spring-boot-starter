package me.alidg.errors.fingerprint;

import me.alidg.errors.FingerprintProvider;
import me.alidg.errors.HttpError;
import org.springframework.lang.NonNull;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

public class MD5FingerprintProvider implements FingerprintProvider {

    @Override
    public String generate(@NonNull HttpError httpError) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String exceptionClass = Optional.ofNullable(httpError.getOriginalException())
                .map(Throwable::getClass)
                .map(Class::getName)
                .orElse("null");

        write(baos, exceptionClass);
        write(baos, System.currentTimeMillis());
        return DigestUtils.md5DigestAsHex(baos.toByteArray());
    }

    private void write(OutputStream os, String s) {
        try {
            os.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ignore
        }
    }

    private void write(OutputStream os, long timestamp) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(0, timestamp);
            os.write(buffer.array());
        } catch (IOException e) {
            // ignore
        }
    }
}

package me.alidg.errors.fingerprint;

import me.alidg.errors.HttpError;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Md5FingerprintProvider} fingerprint provider.
 *
 * @author Ali Dehghani
 */
public class Md5FingerprintProviderTest {

    /**
     * Subject under test.
     */
    private final Md5FingerprintProvider fingerprintProvider = new Md5FingerprintProvider();

    @Test
    public void generate_ShouldGenerateUniqueMd5HashesForSameExceptions() {
        Set<String> generatedFingerprints = new HashSet<>();

        HttpError httpError = exception(new RuntimeException());

        for (int i = 0; i < 100; i++) {
            generatedFingerprints.add(fingerprintProvider.generate(httpError));
        }

        assertThat(generatedFingerprints).hasSize(100);
    }

    @Test
    public void generate_ShouldGenerateUniqueMd5HashesForNullExceptions() {
        Set<String> generatedFingerprints = new HashSet<>();

        HttpError httpError = exception(null);

        for (int i = 0; i < 100; i++) {
            generatedFingerprints.add(fingerprintProvider.generate(httpError));
        }

        assertThat(generatedFingerprints).hasSize(100);
    }

    @Test
    public void generate_ShouldGenerateDifferentFingerprintsForDifferentExceptions() {
        String first = fingerprintProvider.generate(exception(new NullPointerException()));
        String second = fingerprintProvider.generate(exception(new IllegalArgumentException()));

        assertThat(first).isNotEqualTo(second);
    }

    private HttpError exception(Throwable throwable) {
        HttpError httpError = mock(HttpError.class);
        when(httpError.getOriginalException()).thenReturn(throwable);

        return httpError;
    }
}
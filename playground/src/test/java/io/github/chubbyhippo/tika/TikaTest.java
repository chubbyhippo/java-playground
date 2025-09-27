package io.github.chubbyhippo.tika;

import org.apache.tika.Tika;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TikaTest {
    @Test
    @DisplayName("should return text/plain mime type from fake pdf file")
    void shouldReturnTextPlainMimeTypeFromFakePdfFile() {
        Tika tika = new Tika();
        try (InputStream is = getClass().getResourceAsStream("/file.pdf")) {
            if (is == null) throw new FileNotFoundException("Resource not found");
            String detect = tika.detect(is);
            assertThat(detect).isEqualTo("text/plain");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

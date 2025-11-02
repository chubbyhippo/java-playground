package io.github.chubbyhippo.mokito;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;

public class MockitoTest {
    @Test
    @DisplayName("test mock files.exists")
    void testMockFilesExists() {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            Path mockPath = Path.of("/test/file.txt");

            filesMock.when(() -> Files.exists(mockPath))
                    .thenReturn(true);

            boolean exists = Files.exists(mockPath);

            Assertions.assertThat(exists).isTrue();

            filesMock.verify(() -> Files.exists(mockPath));
        }
    }
}

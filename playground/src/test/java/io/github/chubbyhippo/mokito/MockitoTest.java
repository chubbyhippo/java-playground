package io.github.chubbyhippo.mokito;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
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


    @Test
    @DisplayName("test mock construction")
    void testMockConstruction() {
        try (MockedConstruction<ExampleClass> mocked = Mockito.mockConstruction(ExampleClass.class,
                (mock, _) -> {
                    Mockito.when(mock.isTrue()).thenReturn(false);
                })) {

            ExampleClass example = new ExampleClass();
            Boolean result = example.isTrue();

            Assertions.assertThat(result).isFalse();
            Assertions.assertThat(mocked.constructed()).hasSize(1);
        }
    }

    private static class ExampleClass {
        public Boolean isTrue() {
            return true;
        }
    }
}

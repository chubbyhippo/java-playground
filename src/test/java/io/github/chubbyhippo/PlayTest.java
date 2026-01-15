package io.github.chubbyhippo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayTest {
  @Test
  @DisplayName("test get hello")
  void testGetHello() {
    Play play = new Play();
    assertThat(play.getHello()).isEqualTo("Hello");
  }
}

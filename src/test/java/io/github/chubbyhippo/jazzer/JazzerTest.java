package io.github.chubbyhippo.jazzer;

import static org.assertj.core.api.Assertions.assertThat;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.WithUtf8Length;

public class JazzerTest {

  @FuzzTest
  void fuzzMe(@NotNull @WithUtf8Length(min = 10, max = 100) String input) {
    System.out.println(input);
    assertThat(input).isNotNull();
  }
}

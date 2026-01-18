package io.github.chubbyhippo.instancio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;

public class InstancioTest {

  record Address(String street, String city) {}

  record User(Long id, String name, List<Address> addresses) {}

  @Test
  void testUserGeneration() {
    // Create a fully populated User object with one line
    User user = Instancio.create(User.class);

    assertThat(user.id()).isNotNull();
    assertThat(user.name()).isNotBlank();
    assertThat(user.addresses()).isNotEmpty();
  }

  @Test
  void testCustomizedUserGeneration() {
    // You can also customize specific fields while letting the rest be random
    User customUser =
        Instancio.of(User.class)
            .set(Select.field(User::name), "Jesus")
            .generate(Select.field(Address::city), gen -> gen.oneOf("Nazareth", "London"))
            .create();

    assertThat(customUser.name()).isEqualTo("Jesus");
    assertThat(customUser.addresses())
        .extracting(Address::city)
        .allMatch(city -> List.of("Nazareth", "London").contains(city));
  }
}

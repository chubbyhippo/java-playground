package io.github.chubbyhippo.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class WireMockTests {

  @RegisterExtension
  static WireMockExtension wireMockServer =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().dynamicPort())
          .build();

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Test
  @DisplayName("test get endpoint")
  void testGetEndpoint() throws Exception {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/users/1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                {
                                  "id": 1,
                                  "name": "John Doe",
                                  "email": "john@example.com"
                                }
                                """)));

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + "/api/users/1"))
            .GET()
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("John Doe");
    assertThat(response.body()).contains("john@example.com");

    wireMockServer.verify(getRequestedFor(urlEqualTo("/api/users/1")));
  }

  @Test
  @DisplayName("test post endpoint")
  void testPostEndpoint() throws Exception {
    wireMockServer.stubFor(
        post(urlEqualTo("/api/users"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(containing("Jane"))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                {
                                  "id": 2,
                                  "name": "Jane Smith",
                                  "message": "User created successfully"
                                }
                                """)));

    var requestBody =
        """
                {
                  "name": "Jane Smith",
                  "email": "jane@example.com"
                }
                """;

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + "/api/users"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(response.body()).contains("Jane Smith");
    assertThat(response.body()).contains("User created successfully");

    wireMockServer.verify(
        postRequestedFor(urlEqualTo("/api/users"))
            .withHeader("Content-Type", equalTo("application/json")));
  }

  @Test
  @DisplayName("test query parameters")
  void testQueryParameters() throws Exception {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/search"))
            .withQueryParam("q", equalTo("wiremock"))
            .withQueryParam("limit", equalTo("10"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        """
                                {
                                  "results": ["result1", "result2"],
                                  "count": 2
                                }
                                """)));

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + "/api/search?q=wiremock&limit=10"))
            .GET()
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("result1");
  }

  @Test
  @DisplayName("test error response")
  void testErrorResponse() throws Exception {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/users/999"))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withBody(
                        """
                                {
                                  "error": "User not found"
                                }
                                """)));

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(wireMockServer.baseUrl() + "/api/users/999"))
            .GET()
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.body()).contains("User not found");
  }
}

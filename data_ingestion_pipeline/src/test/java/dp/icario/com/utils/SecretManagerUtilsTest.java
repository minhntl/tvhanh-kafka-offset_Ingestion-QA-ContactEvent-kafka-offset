package dp.icario.com.utils;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class SecretManagerUtilsTest {

  @Test
  void testCreateSecretsManagerClient() {
    // Arrange
    Region region = Region.US_EAST_1;
    // Act
    SecretsManagerClient client = SecretManagerUtils.createSecretsManagerClient(region);
    // Assert
    assertNotNull(client);
  }

  @Test
  void testGetValue() {
    // Create a mock SecretsManagerClient
    SecretsManagerClient secretsClient = mock(SecretsManagerClient.class);

    // Create a mock GetSecretValueResponse
    GetSecretValueResponse mockResponse = GetSecretValueResponse.builder()
        .secretString("{\"key\": \"value\"}")
        .build();

    // Set up the mock behavior
    when(secretsClient.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(mockResponse);

    String secretName = "mySecret";
    Region region = Region.US_WEST_2;

    // mock the static createSecretsManagerClient method
    MockedStatic<SecretManagerUtils> mockedStatic = Mockito.mockStatic(SecretManagerUtils.class);
    Mockito.when(SecretManagerUtils.createSecretsManagerClient(region)).thenReturn(secretsClient);
    Mockito.when(SecretManagerUtils.getValue(secretName, region)).thenCallRealMethod();
    // Call the method we want to test
    JSONObject result = SecretManagerUtils.getValue(secretName, region);

    // Verify the behavior
    verify(secretsClient).getSecretValue(any(GetSecretValueRequest.class));

    // Assert the result
    JSONObject expected = new JSONObject("{\"key\": \"value\"}");
    assertEquals(expected.toString(), result.toString());
    mockedStatic.close();
  }
}

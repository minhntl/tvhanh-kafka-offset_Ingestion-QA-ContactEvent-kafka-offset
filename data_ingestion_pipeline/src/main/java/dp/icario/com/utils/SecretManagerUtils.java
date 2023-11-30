package dp.icario.com.utils;

import org.json.JSONObject;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

public class SecretManagerUtils {
  /**
   * This method connects to AWS Secret Manager.
   * You can uncomment .credentialsProvider(credentialsProvider) if you run code in your local machine.
   *
   * @param secretName the secret name you need to get the information.
   * @return a JSON object of the information.
   */
  public static JSONObject getValue(String secretName, Region region) {
    SdkHttpClient urlHttpClient = UrlConnectionHttpClient.create();
    try (SecretsManagerClient secretsClient = createSecretsManagerClient(region)) {
      GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
          .secretId(secretName)
          .build();

      GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
      String secret = valueResponse.secretString();

      return new JSONObject(secret);

    } catch (SecretsManagerException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
      return null;
    }
  }

  protected static SecretsManagerClient createSecretsManagerClient(Region region) {
    SdkHttpClient urlHttpClient = UrlConnectionHttpClient.create();
    return SecretsManagerClient.builder()
        .region(region)
        .httpClient(urlHttpClient)
        .build();
  }
}

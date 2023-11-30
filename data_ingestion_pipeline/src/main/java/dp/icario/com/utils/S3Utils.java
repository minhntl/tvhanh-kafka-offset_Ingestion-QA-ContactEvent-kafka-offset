package dp.icario.com.utils;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.List;
import java.util.stream.Collectors;

public class S3Utils {
  public static void deleteObjectsInBucket(Region region, String bucket, String object) {
    S3Client s3 = createS3Client(region);
    ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(object)
        .build();
    ListObjectsV2Iterable paginatedListResponse = s3.listObjectsV2Paginator(listRequest);

    for (ListObjectsV2Response listResponse : paginatedListResponse) {
      List<ObjectIdentifier> objects = listResponse.contents().stream()
          .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
          .collect(Collectors.toList());
      if (objects.isEmpty()) {
        break;
      }
      DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
          .bucket(bucket)
          .delete(Delete.builder().objects(objects).build())
          .build();
      s3.deleteObjects(deleteRequest);
    }
  }

  protected static S3Client createS3Client(Region region) {
    SdkHttpClient apacheHttpClient = ApacheHttpClient.create();
    return S3Client.builder()
        .region(region)
        .httpClient(apacheHttpClient)
        .build();
  }
}

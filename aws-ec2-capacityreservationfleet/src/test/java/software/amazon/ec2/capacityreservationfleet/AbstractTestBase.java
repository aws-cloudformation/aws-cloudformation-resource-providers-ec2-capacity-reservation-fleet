package software.amazon.ec2.capacityreservationfleet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.awssdk.services.ec2.model.FleetCapacityReservation;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();
  }
  static ProxyClient<Ec2Client> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final Ec2Client ec2Client) {
    return new ProxyClient<Ec2Client>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Ec2Client client() {
        return ec2Client;
      }
    };
  }

  public static DescribeCapacityReservationFleetsResponse translateToDescribeCapacityReservationFleetsResponse(final ResourceModel model) {
    final DescribeCapacityReservationFleetsResponse.Builder builder = DescribeCapacityReservationFleetsResponse.builder();
    final List<FleetCapacityReservation> instanceTypeSpecifications = new ArrayList<>();

    model.getInstanceTypeSpecifications().stream().forEach(specification -> {
      instanceTypeSpecifications.add(FleetCapacityReservation.builder()
              .instanceType(specification.getInstanceType())
              .instancePlatform(specification.getInstancePlatform())
              .availabilityZone(specification.getAvailabilityZone())
              .availabilityZoneId(specification.getAvailabilityZoneId())
              .ebsOptimized(specification.getEbsOptimized())
              .priority(specification.getPriority())
              .weight(specification.getWeight())
              .build());
    });

    builder.capacityReservationFleets(Arrays.asList(software.amazon.awssdk.services.ec2.model.CapacityReservationFleet.builder()
            .capacityReservationFleetId(model.getCapacityReservationFleetId())
            .allocationStrategy(model.getAllocationStrategy())
            .instanceMatchCriteria(model.getInstanceMatchCriteria())
            .tenancy(model.getTenancy())
            .totalTargetCapacity(model.getTotalTargetCapacity())
            .instanceTypeSpecifications(instanceTypeSpecifications)
            .state(CapacityReservationFleetState.ACTIVE)
            .build()));

    return builder.build();
  }

  public DescribeCapacityReservationFleetsResponse createDescribeResponse(final CapacityReservationFleetState state, final String crFleetId) {
    return DescribeCapacityReservationFleetsResponse.builder()
            .capacityReservationFleets(Arrays.asList(software.amazon.awssdk.services.ec2.model.CapacityReservationFleet.builder()
                    .state(state).capacityReservationFleetId(crFleetId)
                    .instanceTypeSpecifications(Arrays.asList(FleetCapacityReservation.builder()
                            .instanceType("m4.xlarge")
                            .availabilityZone("us-east-1")
                            .instancePlatform("linux")
                            .ebsOptimized(true)
                            .build()))
                    .totalTargetCapacity(1)
                    .allocationStrategy("prioritized")
                    .instanceMatchCriteria("open")
                    .build()))
            .build();
  }
}

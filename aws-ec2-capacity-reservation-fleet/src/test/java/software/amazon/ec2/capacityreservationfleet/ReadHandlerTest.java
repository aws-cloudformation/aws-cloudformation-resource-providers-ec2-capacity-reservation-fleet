package software.amazon.ec2.capacityreservationfleet;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.ec2.capacityreservationfleet.Translator.INVALID_CR_FLEET_ID_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<Ec2Client> proxyClient;

    @Mock
    Ec2Client ec2Client;

    private ResourceModel model;
    private final String crFleetId = "crf-1234";

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ec2Client = mock(Ec2Client.class);
        proxyClient = MOCK_PROXY(proxy, ec2Client);
        model = ResourceModel.builder()
                .capacityReservationFleetId(crFleetId)
                .instanceTypeSpecifications(Arrays.asList(InstanceTypeSpecification.builder()
                        .instanceType("m4.xlarge")
                        .availabilityZone("us-east-1")
                        .instancePlatform("linux")
                        .ebsOptimized(true)
                        .build()))
                .totalTargetCapacity(1)
                .allocationStrategy("prioritized")
                .instanceMatchCriteria("open")
                .build();
    }

    @AfterEach
    public void tear_down() {
        verify(ec2Client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(ec2Client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final DescribeCapacityReservationFleetsResponse describeCapacityReservationFleetsResponse = translateToDescribeCapacityReservationFleetsResponse(model);
        when(proxyClient.client().describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class))).thenReturn(describeCapacityReservationFleetsResponse);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_whenDescribeCRFleets_throwsException() throws Exception {
        final ReadHandler handler = new ReadHandler();
        final AwsServiceException serviceException = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(HandlerErrorCode.GeneralServiceException.toString()).build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        when(proxyClient.client().describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class))).thenThrow(serviceException);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_whenDescribeCRFleets_throwsNotFoundException() throws Exception {
        final ReadHandler handler = new ReadHandler();
        final AwsServiceException serviceException = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INVALID_CR_FLEET_ID_NOT_FOUND).build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        when(proxyClient.client().describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class))).thenThrow(serviceException);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}

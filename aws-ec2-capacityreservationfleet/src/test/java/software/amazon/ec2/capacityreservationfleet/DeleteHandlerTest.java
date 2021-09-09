package software.amazon.ec2.capacityreservationfleet;

import java.time.Duration;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CancelCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.CancelCapacityReservationFleetsResponse;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleet;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetCancellationState;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.awssdk.services.ec2.model.FailedCapacityReservationFleetCancellationResult;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.NotStabilized;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.ServiceInternalError;
import static software.amazon.ec2.capacityreservationfleet.Translator.UNAUTHORIZED_CODE;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<Ec2Client> proxyClient;

    @Mock
    Ec2Client ec2Client;
    private final String crFleetId = "crf-1234";

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ec2Client = mock(Ec2Client.class);
        proxyClient = MOCK_PROXY(proxy, ec2Client);
    }

    @AfterEach
    public void tear_down() {
        verify(ec2Client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(ec2Client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final DescribeCapacityReservationFleetsResponse describeResponse = DescribeCapacityReservationFleetsResponse.builder()
                .capacityReservationFleets(Arrays.asList(software.amazon.awssdk.services.ec2.model.CapacityReservationFleet.builder()
                        .state(CapacityReservationFleetState.CANCELLED).capacityReservationFleetId(crFleetId)
                        .totalTargetCapacity(5)
                        .build()))
                .build();

        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build())
                .thenReturn(describeResponse);
        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenReturn(CancelCapacityReservationFleetsResponse.builder()
                .successfulFleetCancellations(CapacityReservationFleetCancellationState.builder().capacityReservationFleetId(crFleetId).build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));
        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_handleError_WhenCancelCRFleetThrowsException() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build());

        final AwsServiceException serviceException = AwsServiceException.builder().message("serviceException").build();
        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenThrow(serviceException);

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(result.getResourceModel()).isNull();
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getMessage()).containsIgnoringCase("serviceException");
        assertThat(result.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_handleError_WhenResponseIsMissing() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build());
        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenReturn(null);

        try {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        } catch (CfnNotStabilizedException ex) {
            assertThat(ex.getErrorCode()).isEqualTo(NotStabilized);
        }

        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));
    }

    @Test
    public void handleRequest_SuccessAfterRetryOnStabilize() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final DescribeCapacityReservationFleetsResponse describeResponseInCancelling = DescribeCapacityReservationFleetsResponse.builder()
                .capacityReservationFleets(Arrays.asList(software.amazon.awssdk.services.ec2.model.CapacityReservationFleet.builder()
                        .state(CapacityReservationFleetState.CANCELLING).capacityReservationFleetId(crFleetId)
                        .totalTargetCapacity(5)
                        .build()))
                .build();
        final DescribeCapacityReservationFleetsResponse describeResponseInCancelled = DescribeCapacityReservationFleetsResponse.builder()
                .capacityReservationFleets(Arrays.asList(software.amazon.awssdk.services.ec2.model.CapacityReservationFleet.builder()
                        .state(CapacityReservationFleetState.CANCELLED).capacityReservationFleetId(crFleetId)
                        .totalTargetCapacity(5)
                        .build()))
                .build();
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build())
                .thenReturn(describeResponseInCancelling)
                .thenReturn(describeResponseInCancelled);
        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenReturn(CancelCapacityReservationFleetsResponse.builder()
                .successfulFleetCancellations(CapacityReservationFleetCancellationState.builder().capacityReservationFleetId(crFleetId).build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));
        verify(ec2Client, atLeast(2)).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_handleError_whenStabilizeFail() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenReturn(CancelCapacityReservationFleetsResponse.builder()
                .successfulFleetCancellations(CapacityReservationFleetCancellationState.builder().capacityReservationFleetId(crFleetId).build()).build());
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build())
                .thenThrow(AwsServiceException.builder().message("serviceException").build());

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));
        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_handleError_whenDescribeInitResponseIsBroken() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenReturn(CancelCapacityReservationFleetsResponse.builder()
                .successfulFleetCancellations(CapacityReservationFleetCancellationState.builder().capacityReservationFleetId(crFleetId).build()).build());
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build())
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().build())
                .thenReturn(createDescribeResponse(CapacityReservationFleetState.CANCELLED, crFleetId));

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));
        verify(ec2Client, times(3)).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_handleError_whenFailedCancellation() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build());

        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenReturn(CancelCapacityReservationFleetsResponse.builder()
                .failedFleetCancellations(Arrays.asList(FailedCapacityReservationFleetCancellationResult.builder().capacityReservationFleetId("crf-1234").build()))
                .successfulFleetCancellations(CapacityReservationFleetCancellationState.builder().capacityReservationFleetId(crFleetId).build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(ServiceInternalError);
    }

    @Test
    public void handleRequest_handleError_whenDescribeResponseFailByUnauthorizedException() {
        final DeleteHandler handler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().capacityReservationFleetId(crFleetId).build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(ec2Client.cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class))).thenReturn(CancelCapacityReservationFleetsResponse.builder()
                .successfulFleetCancellations(CapacityReservationFleetCancellationState.builder().capacityReservationFleetId(crFleetId).build()).build());
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().capacityReservationFleets(Arrays.asList(CapacityReservationFleet.builder().capacityReservationFleetId("crf-1234").state(CapacityReservationFleetState.ACTIVE).build())).build())
                .thenThrow(AwsServiceException.builder().message("err").awsErrorDetails(AwsErrorDetails.builder().errorCode(UNAUTHORIZED_CODE).build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        verify(ec2Client, atLeastOnce()).cancelCapacityReservationFleets(any(CancelCapacityReservationFleetsRequest.class));
        verify(ec2Client, times(2)).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}

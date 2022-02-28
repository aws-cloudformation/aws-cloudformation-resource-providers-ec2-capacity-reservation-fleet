package software.amazon.ec2.capacityreservationfleet;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.CreateCapacityReservationFleetRequest;
import software.amazon.awssdk.services.ec2.model.CreateCapacityReservationFleetResponse;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.awssdk.services.ec2.model.FleetCapacityReservation;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.NotFound;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.NotStabilized;
import static software.amazon.ec2.capacityreservationfleet.Translator.UNAUTHORIZED_CODE;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

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
                .instanceTypeSpecifications(new HashSet<>(Arrays.asList(InstanceTypeSpecification.builder()
                        .instanceType("m4.xlarge")
                        .availabilityZone("us-east-1")
                        .instancePlatform("linux")
                        .ebsOptimized(true)
                        .build())))
                .totalTargetCapacity(1)
                .allocationStrategy("prioritized")
                .instanceMatchCriteria("open")
                .capacityReservationFleetId(crFleetId)
                .tagSpecifications(Arrays.asList(TagSpecification.builder().tags(Arrays.asList(Tag.builder()
                                .key("test key")
                                .value("Test")
                                .build()))
                        .resourceType("capacity-reservation-fleet").build(),
                        TagSpecification.builder()
                                .tags(Arrays.asList(Tag.builder()
                                        .key("TestKey2")
                                        .value("TestValue2").build()
                                ))
                                .resourceType("ec2-instance").build()))
                .build();
    }

    @AfterEach
    public void tear_down() {
        verify(ec2Client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(ec2Client);
    }

    @Test
    public void handleRequest_SimpleSuccess_withTags() {
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateCapacityReservationFleetResponse response = CreateCapacityReservationFleetResponse.builder()
                .capacityReservationFleetId(crFleetId).state(CapacityReservationFleetState.SUBMITTED).build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenReturn(response);
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class))).thenReturn(createDescribeResponse(CapacityReservationFleetState.ACTIVE, crFleetId));

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(result.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_withoutTags() {
        model.setTagSpecifications(null);
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateCapacityReservationFleetResponse response = CreateCapacityReservationFleetResponse.builder()
                .capacityReservationFleetId(crFleetId).state(CapacityReservationFleetState.SUBMITTED).build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenReturn(response);
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class))).thenReturn(createDescribeResponse(CapacityReservationFleetState.ACTIVE, crFleetId));

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(result.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_handleError_WhenCreateCRFleetThrowsException() {
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final AwsServiceException serviceException = AwsServiceException.builder().message("serviceException").build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenThrow(serviceException);

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(result.getResourceModel()).isNull();
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getMessage()).containsIgnoringCase("serviceException");
        assertThat(result.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

    }

    @Test
    public void handleRequest_handleError_whenCrFleetIdIsMissing() {
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateCapacityReservationFleetResponse response = CreateCapacityReservationFleetResponse.builder()
                .state(CapacityReservationFleetState.SUBMITTED).build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenReturn(response);

        try {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        } catch (CfnNotFoundException ex) {
            assertThat(ex.getErrorCode()).isEqualTo(NotFound);
        }

        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
    }

    @Test
    public void handleRequest_handleError_whenStabilizeFail() {
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateCapacityReservationFleetResponse response = CreateCapacityReservationFleetResponse.builder()
                .capacityReservationFleetId(crFleetId).state(CapacityReservationFleetState.SUBMITTED).build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenReturn(response);
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenThrow(AwsServiceException.builder().message("serviceException").build());

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
        verify(ec2Client, atLeastOnce()).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_handleError_whenDescribeInitResponseIsBroken() {
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateCapacityReservationFleetResponse response = CreateCapacityReservationFleetResponse.builder()
                .capacityReservationFleetId(crFleetId).state(CapacityReservationFleetState.SUBMITTED).build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenReturn(response);
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(DescribeCapacityReservationFleetsResponse.builder().build())
                .thenReturn(createDescribeResponse(CapacityReservationFleetState.ACTIVE, crFleetId));

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
        verify(ec2Client, times(3)).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(result.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_success_after_stabilization() {
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateCapacityReservationFleetResponse response = CreateCapacityReservationFleetResponse.builder()
                .capacityReservationFleetId(crFleetId).state(CapacityReservationFleetState.SUBMITTED).build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenReturn(response);


        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenReturn(createDescribeResponse(CapacityReservationFleetState.SUBMITTED, crFleetId))
                .thenReturn(createDescribeResponse(CapacityReservationFleetState.ACTIVE, crFleetId));

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
        verify(ec2Client, times(3)).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(result.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_handleError_whenDescribeResponseFailByUnauthorizedException() {
        final CreateHandler handler = new CreateHandler();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateCapacityReservationFleetResponse response = CreateCapacityReservationFleetResponse.builder()
                .capacityReservationFleetId(crFleetId).state(CapacityReservationFleetState.SUBMITTED).build();
        when(ec2Client.createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class))).thenReturn(response);
        when(ec2Client.describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class)))
                .thenThrow(AwsServiceException.builder().message("err").awsErrorDetails(AwsErrorDetails.builder().errorCode(UNAUTHORIZED_CODE).build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> result = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        verify(ec2Client, atLeastOnce()).createCapacityReservationFleet(any(CreateCapacityReservationFleetRequest.class));
        verify(ec2Client, times(1)).describeCapacityReservationFleets(any(DescribeCapacityReservationFleetsRequest.class));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getCallbackDelaySeconds()).isEqualTo(0);
        Assertions.assertThat(result.getResourceModels()).isNull();
        assertThat(result.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}

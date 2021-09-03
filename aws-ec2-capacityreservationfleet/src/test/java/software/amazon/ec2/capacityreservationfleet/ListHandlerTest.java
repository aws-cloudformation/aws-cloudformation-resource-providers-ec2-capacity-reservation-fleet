package software.amazon.ec2.capacityreservationfleet;

import org.assertj.core.api.Assertions;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleet;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.ec2.capacityreservationfleet.ListHandler;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CapacityReservationFleet fleet1 = CapacityReservationFleet.builder().capacityReservationFleetId("crf-1").state(CapacityReservationFleetState.ACTIVE).build();
        final CapacityReservationFleet fleet2 = CapacityReservationFleet.builder().capacityReservationFleetId("crf-2").state(CapacityReservationFleetState.ACTIVE).build();
        final DescribeCapacityReservationFleetsResponse describeCRFleetsResponse = DescribeCapacityReservationFleetsResponse.builder()
                .capacityReservationFleets(Arrays.asList(fleet1, fleet2)).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenReturn(describeCRFleetsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        final ResourceModel model1 = ResourceModel.builder().capacityReservationFleetId("crf-1").build();
        final ResourceModel model2 = ResourceModel.builder().capacityReservationFleetId("crf-2").build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNotNull();
        Assertions.assertThat(response.getResourceModels()).containsExactly(model1, model2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_sdkException_returnsFailed() {
        final ListHandler handler = new ListHandler();
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final SdkException ex = AwsServiceException.builder().message("exceptionMessage").build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenThrow(ex);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).containsIgnoringCase("exceptionMessage");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }
}

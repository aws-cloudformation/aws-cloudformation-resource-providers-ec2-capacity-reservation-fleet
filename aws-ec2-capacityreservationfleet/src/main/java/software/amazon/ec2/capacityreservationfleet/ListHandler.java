package software.amazon.ec2.capacityreservationfleet;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, software.amazon.ec2.capacityreservationfleet.CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final software.amazon.ec2.capacityreservationfleet.CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        List<ResourceModel> models;
        final DescribeCapacityReservationFleetsRequest describeCapacityReservationFleetsRequest = Translator.translateToListRequest(request.getNextToken());
        String nextToken = null;

        try {
            DescribeCapacityReservationFleetsResponse response =
                    proxy.injectCredentialsAndInvokeV2(describeCapacityReservationFleetsRequest, (proxyRequest) -> proxyClient.client().describeCapacityReservationFleets(proxyRequest));
            nextToken = response.nextToken();
            models = Translator.translateFromListRequest(response);
        } catch (final SdkException e) {
            logger.log(String.format("[INFO] EC2CapacityReservation ListHandler threw a sdk exception: %s", e));
            return Translator.translateToFailure(e);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(nextToken)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}

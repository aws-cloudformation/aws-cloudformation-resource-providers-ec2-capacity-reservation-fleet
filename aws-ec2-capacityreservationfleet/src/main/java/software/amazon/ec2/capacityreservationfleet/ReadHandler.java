package software.amazon.ec2.capacityreservationfleet;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, software.amazon.ec2.capacityreservationfleet.CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-EC2-CapacityReservationFleet::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(model -> Translator.translateToReadRequest(model, logger))
                .makeServiceCall((describeRequest, ec2ClientProxyClient) -> describeCapacityReservationFleets(describeRequest, ec2ClientProxyClient, logger))
                .handleError((awsRequest, exception, client, model, context) -> handleDescribeCapacityReservationFleetsError(awsRequest, exception, proxyClient, model, context))
                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse, logger, request.getDesiredResourceState(), true)));
    }
}

package software.amazon.ec2.capacityreservationfleet;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleet;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.awssdk.services.ec2.model.ModifyCapacityReservationFleetResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, software.amazon.ec2.capacityreservationfleet.CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {
        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-EC2-CapacityReservationFleet::Update-exist", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest((model) -> Translator.translateToReadRequest(model, logger))
                        .makeServiceCall((describeRequest, ec2ClientProxyClient) -> describeCapacityReservationFleets(describeRequest, ec2ClientProxyClient, logger))
                        .handleError((awsRequest, exception, client, model, context) -> handleDescribeCapacityReservationFleetsError(awsRequest, exception, proxyClient, model, context))
                        .done((describeFleetsRequest, describeFleetsResponse, client, model, context) ->
                                Translator.translateToResourceFoundProgress(describeFleetsResponse, logger, context, model, true)))
            .then(progress ->
                proxy.initiate("AWS-EC2-CapacityReservationFleet::Update-update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest((model) -> Translator.translateToUpdateRequest(model, logger))
                        .makeServiceCall((awsRequest, ec2ClientProxyClient) -> {
                            ModifyCapacityReservationFleetResponse response = null;
                            try {
                                logger.log(String.format("[INFO] Calling modifyCapacityReservationFleet for update with request: %s", request));
                                response = ec2ClientProxyClient.injectCredentialsAndInvokeV2(awsRequest, ec2ClientProxyClient.client()::modifyCapacityReservationFleet);
                                logger.log(String.format("[INFO] modifyCapacityReservationFleet response: %s", response));

                                if (!response.returnValue()) {
                                    logger.log(String.format("[ERROR] ModifyCapacityReservationFleet request failed. crFleetId: %s", awsRequest.capacityReservationFleetId()));
                                    throw AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder()
                                            .errorMessage("ModifyCapacityReservationFleet failed.").build()).statusCode(500).build();
                                }

                                return response;
                            } catch (final AwsServiceException e) {
                                logger.log(String.format("[INFO] Exception thrown while modifying fleet request: %s", e));
                                throw e;
                            }
                        })
                        .stabilize((awsRequest, awsResponse, client, model, context) -> {
                            boolean stabilized = false;
                            try {
                                final DescribeCapacityReservationFleetsRequest describeCapacityReservationFleetsRequest = Translator.translateToReadRequest(model, logger);
                                final DescribeCapacityReservationFleetsResponse describeCapacityReservationFleetsResponse = describeCapacityReservationFleets(describeCapacityReservationFleetsRequest, proxyClient, logger);

                                if (describeCapacityReservationFleetsResponse.hasCapacityReservationFleets()) {
                                    final CapacityReservationFleet crFleet = describeCapacityReservationFleetsResponse.capacityReservationFleets().get(0);
                                    stabilized = (CapacityReservationFleetState.ACTIVE.equals(crFleet.state()) || CapacityReservationFleetState.PARTIALLY_FULFILLED.equals(crFleet.state()));
                                    logger.log(String.format("[INFO] Modified cr fleet is in %s state. Stabilized: ", crFleet.state(), stabilized));
                                }

                                return stabilized;
                            } catch (final AwsServiceException ex) {
                                logger.log(String.format("[ERROR] A exception occurred during stabilization: %s", ex));

                                if (isUnauthorizedException(ex)) {
                                    logger.log(String.format("[INFO] User is missing permissions for DescribeCapacityReservationFleets during Update."));
                                }

                                throw ex;
                            }
                        })
                        .handleError((awsRequest, exception, client, model, context) -> Translator.translateToFailure(exception))
                        .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}

package software.amazon.ec2.capacityreservationfleet;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CancelCapacityReservationFleetsResponse;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleet;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-EC2-CapacityReservationFleet::Delete-exist", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest((model) -> Translator.translateToReadRequest(model, logger))
                                .makeServiceCall((describeRequest, ec2ClientProxyClient) -> describeCapacityReservationFleets(describeRequest, ec2ClientProxyClient, logger))
                                .handleError((awsRequest, exception, client, model, context) -> handleDescribeCapacityReservationFleetsError(awsRequest, exception, proxyClient, model, context))
                                .done((describeFleetsRequest, describeFleetsResponse, client, model, context) ->
                                        Translator.translateToResourceFoundProgress(describeFleetsResponse, logger, context, model, false)))
                .then(progress ->
                        proxy.initiate("AWS-EC2-CapacityReservationFleet::Delete-delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest((model) -> Translator.translateToDeleteRequest(model, logger))
                                .makeServiceCall((awsRequest, client) -> {
                                    CancelCapacityReservationFleetsResponse response = null;
                                    final String crFleetId = awsRequest.capacityReservationFleetIds().get(0);

                                    try {
                                        logger.log(String.format("[INFO] Calling CancelCapacityReservationFleets: %s", awsRequest));
                                        response = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::cancelCapacityReservationFleets);
                                        logger.log(String.format("[INFO] Successfully cancelled CRFleet: %s", crFleetId));

                                        return response;
                                    } catch (final AwsServiceException e) {
                                        logger.log(String.format("[INFO] CancelCapacityFleets for fleet %s threw an exception: %s", crFleetId, e));

                                        throw e;
                                    }
                                })
                                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                                    boolean stabilized = false;
                                    try {
                                        if (awsResponse == null) {
                                            throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, awsRequest.capacityReservationFleetIds().get(0));
                                        } else if (awsResponse.hasFailedFleetCancellations() && awsResponse.failedFleetCancellations().size() > 0) {
                                            logger.log("[ERROR] CR Fleet cancellation failed: " + awsResponse.failedFleetCancellations());
                                            throw AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder()
                                                    .errorMessage("CancelCapacityReservationFleets failed.").build()).statusCode(500).build();
                                        }

                                        final DescribeCapacityReservationFleetsRequest describeCapacityReservationFleetsRequest = Translator.translateToReadRequest(model, logger);
                                        final DescribeCapacityReservationFleetsResponse describeCapacityReservationFleetsResponse = describeCapacityReservationFleets(describeCapacityReservationFleetsRequest, proxyClient, logger);

                                        if (describeCapacityReservationFleetsResponse.hasCapacityReservationFleets()) {
                                            final CapacityReservationFleet crFleet = describeCapacityReservationFleetsResponse.capacityReservationFleets().get(0);
                                            stabilized = CapacityReservationFleetState.CANCELLED.equals(crFleet.state());
                                            logger.log(String.format("[INFO] Cancel requested cr fleet is in %s state. Stabilized: %s", crFleet.state(), stabilized));
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
                                .done(response -> ProgressEvent.success(null, callbackContext)));
    }
}

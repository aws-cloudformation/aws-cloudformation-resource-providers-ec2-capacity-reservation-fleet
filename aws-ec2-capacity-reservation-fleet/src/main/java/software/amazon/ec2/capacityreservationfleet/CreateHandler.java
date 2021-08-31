package software.amazon.ec2.capacityreservationfleet;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleet;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.awssdk.services.ec2.model.CreateCapacityReservationFleetResponse;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
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
                        proxy.initiate("AWS-EC2-CapacityReservationFleet::Create", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest((model) -> Translator.translateToCreateRequest(model, request, logger))
                                .makeServiceCall((awsRequest, client) -> {
                                    logger.log(String.format("[INFO] Creating resource with CreateCapacityReservationFleet: %s", request));

                                    CreateCapacityReservationFleetResponse response = null;
                                    try {
                                        response = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createCapacityReservationFleet);
                                        logger.log(String.format("[INFO] createCapacityReservationFleet response : ", response));

                                        return response;
                                    } catch (final SdkException e) {
                                        logger.log(String.format("[INFO] Encountered an exception while calling CreateCapacityReservationFleet to create resource: ", e));
                                        throw e;
                                    }
                                })
                                .stabilize((awsRequest, awsResponse, client, model, context) -> {
                                    boolean stabilized = false;
                                    try {
                                        logger.log(String.format("[INFO] stabilizing crFleet %s in state %s ", awsResponse.capacityReservationFleetId(), awsResponse.state()));
                                        model.setCapacityReservationFleetId(awsResponse.capacityReservationFleetId());
                                        final DescribeCapacityReservationFleetsRequest describeCapacityReservationFleetsRequest = Translator.translateToReadRequest(model);
                                        final DescribeCapacityReservationFleetsResponse describeCapacityReservationFleetsResponse = describeCapacityReservationFleets(describeCapacityReservationFleetsRequest, proxyClient, logger);

                                        if (describeCapacityReservationFleetsResponse.hasCapacityReservationFleets()) {
                                            final CapacityReservationFleet crFleet = describeCapacityReservationFleetsResponse.capacityReservationFleets().get(0);
                                            stabilized = (CapacityReservationFleetState.ACTIVE.equals(crFleet.state()) || CapacityReservationFleetState.PARTIALLY_FULFILLED.equals(crFleet.state()));
                                            logger.log(String.format("[INFO] cr fleet is in %s state. Stabilized: %s", crFleet.state(), stabilized));
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
                                .progress()
                )
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}

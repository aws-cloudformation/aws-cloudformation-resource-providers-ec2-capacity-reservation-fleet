package software.amazon.ec2.capacityreservationfleet;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.ec2.capacityreservationfleet.Translator.UNAUTHORIZED_CODE;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {


  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          final AmazonWebServicesClientProxy proxy,
          final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext callbackContext,
          final Logger logger) {
    return handleRequest(
            proxy,
            request,
            callbackContext != null ? callbackContext : new CallbackContext(),
            proxy.newProxy(ClientBuilder::getClient),
            logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          final AmazonWebServicesClientProxy proxy,
          final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext callbackContext,
          final ProxyClient<Ec2Client> proxyClient,
          final Logger logger);

  /**
   *  describe api is used in almost all handlers so defined here for common usage.
   *
   * @param request
   * @param proxyClient
   * @param logger
   * @return
   */
  protected DescribeCapacityReservationFleetsResponse describeCapacityReservationFleets(
          final DescribeCapacityReservationFleetsRequest request,
          final ProxyClient<Ec2Client> proxyClient,
          final Logger logger) {
    DescribeCapacityReservationFleetsResponse response = null;
    try {
      logger.log(String.format("[INFO] Trying DescribeCapacityReservationFleets with request: %s", request));
      response = proxyClient.injectCredentialsAndInvokeV2(request, (proxyRequest) -> proxyClient.client().describeCapacityReservationFleets(proxyRequest));

      final String requestId = response.responseMetadata() != null ? response.responseMetadata().requestId() : null;
      logger.log(String.format("[INFO] Received DescribeCapacityReservationFleets requestId: %s response: %s", requestId, response));
    } catch (final AwsServiceException e) {
      logger.log(String.format("[INFO] Exception thrown while describing CapacityReservationfleet with request: %s", request));
      throw e;
    }

    logger.log(String.format("[INFO] %s has successfully been read.", ResourceModel.TYPE_NAME));
    return response;
  }

  protected ProgressEvent<ResourceModel, CallbackContext> handleDescribeCapacityReservationFleetsError(
          final DescribeCapacityReservationFleetsRequest request,
          final Exception exception,
          final ProxyClient<Ec2Client> proxyClient,
          final ResourceModel model,
          final CallbackContext context
  ) {
    return Translator.translateToFailure(exception);
  }

  protected boolean isUnauthorizedException(final Exception ex) {
    if (!(ex instanceof AwsServiceException)) {
      return false;
    }

    AwsServiceException awsServiceException = (AwsServiceException) ex;

    if (awsServiceException.awsErrorDetails() == null) {
      return false;
    }

    final String errorCode = (awsServiceException.awsErrorDetails().errorCode());

    return UNAUTHORIZED_CODE.equalsIgnoreCase(errorCode);
  }
}

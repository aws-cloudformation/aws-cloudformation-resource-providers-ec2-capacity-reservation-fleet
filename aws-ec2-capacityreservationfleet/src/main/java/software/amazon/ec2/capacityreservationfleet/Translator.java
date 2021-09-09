package software.amazon.ec2.capacityreservationfleet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.model.CancelCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleet;
import software.amazon.awssdk.services.ec2.model.CreateCapacityReservationFleetRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCapacityReservationFleetsResponse;
import software.amazon.awssdk.services.ec2.model.ModifyCapacityReservationFleetRequest;
import software.amazon.awssdk.services.ec2.model.ReservationFleetInstanceSpecification;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.awssdk.services.ec2.model.CapacityReservationFleetState;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {
  private static final String CR_FLEET_TAG_RESOURCE_TYPE = "capacity-reservation-fleet";
  public static final String INVALID_CR_FLEET_ID_NOT_FOUND = "InvalidCapacityReservationFleetId.NotFound";
  public static final String INVALID_CR_FLEET_ID_MALFORMED = "InvalidCapacityReservationFleetId.Malformed";
  public static final String INVALID_CR_FLEET_STATE_TRANSITION = "InvalidCapacityReservationFleetStateTransition";
  public static final String UNAUTHORIZED_CODE = "UnauthorizedOperation";

  /**
   * Request to create a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static CreateCapacityReservationFleetRequest translateToCreateRequest(final ResourceModel model,
                                                    final ResourceHandlerRequest<ResourceModel> handlerRequest,
                                                    final Logger logger) {
    logger.log(String.format("[INFO] translateToCreateRequest: %s", model));
    final CreateCapacityReservationFleetRequest.Builder requestBuilder = CreateCapacityReservationFleetRequest.builder();
      final List<ReservationFleetInstanceSpecification> reservationFleetInstanceSpecifications = new ArrayList<>();
      model.getInstanceTypeSpecifications().stream().forEach(spec -> {
        reservationFleetInstanceSpecifications.add(ReservationFleetInstanceSpecification.builder()
                .instanceType(spec.getInstanceType())
                .instancePlatform(spec.getInstancePlatform())
                .availabilityZone(spec.getAvailabilityZone())
                .availabilityZoneId(spec.getAvailabilityZoneId())
                .ebsOptimized(spec.getEbsOptimized())
                .priority(spec.getPriority())
                .weight(spec.getWeight())
                .build());
      });

      requestBuilder.allocationStrategy(model.getAllocationStrategy());
      requestBuilder.endDate(StringUtils.isBlank(model.getEndDate()) ? null : new Date(Integer.parseInt(model.getEndDate())).toInstant());
      requestBuilder.instanceMatchCriteria(model.getInstanceMatchCriteria());
      requestBuilder.tenancy(model.getTenancy());
      requestBuilder.totalTargetCapacity(model.getTotalTargetCapacity());
      requestBuilder.instanceTypeSpecifications(reservationFleetInstanceSpecifications);

      List<software.amazon.awssdk.services.ec2.model.TagSpecification> tags = getTags(handlerRequest, model, logger);

      if (tags != null && tags.size() > 0) {
        requestBuilder.tagSpecifications(tags);
      }

      return requestBuilder.build();

  }

  /**
   * Request to read a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  public static DescribeCapacityReservationFleetsRequest translateToReadRequest(final ResourceModel model, final Logger logger) {
    logger.log(String.format("[INFO] translateToReadRequest : %s", model));
    final String crFleetId = model.getCapacityReservationFleetId();
    if (crFleetId == null) {
      throw new CfnNotFoundException(ResourceModel.TYPE_NAME, null);
    }

    return DescribeCapacityReservationFleetsRequest.builder()
            .capacityReservationFleetIds(model.getCapacityReservationFleetId())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model.
   * It is written to handle single resource at a time.
   *
   * Return only SUCCESS for FAILED status as per the official guide - https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
   *
   * @param response the aws service describe resource response
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final DescribeCapacityReservationFleetsResponse response,
                                                          final Logger logger,
                                                          final ResourceModel desiredResourceState) {
    if (desiredResourceState == null) {
      logger.log("[ERROR] desiredResourceState is null");
      throw new CfnServiceInternalErrorException("Resource is not in a desired state.");
    }

    validateReadResponse(response, logger);
    final ResourceModel.ResourceModelBuilder builder = ResourceModel.builder();
    final CapacityReservationFleet crFleet = response.capacityReservationFleets().get(0);
    final List<Tag> tags = crFleet.tags().stream().map((tag) -> Tag.builder().key(tag.key()).value(tag.value()).build()).collect(Collectors.toList());
    List<TagSpecification> tagSpecifications = null;

    if (desiredResourceState.getTagSpecifications() != null) {
      tagSpecifications = desiredResourceState.getTagSpecifications();
    } else if (!CollectionUtils.isEmpty(tags)) {
      tagSpecifications = Arrays.asList(TagSpecification.builder().resourceType(CR_FLEET_TAG_RESOURCE_TYPE).tags(tags).build());
    }

    final List<InstanceTypeSpecification> instanceTypeSpecifications = new ArrayList<>();

    logger.log("[INFO] Mapping DescribeCapacityReservationFleets response to ResourceModel");

    crFleet.instanceTypeSpecifications().stream().forEach(specification -> {
      instanceTypeSpecifications.add(InstanceTypeSpecification.builder()
              .instanceType(specification.instanceTypeAsString())
              .instancePlatform(specification.instancePlatformAsString())
              .availabilityZone(specification.availabilityZone())
              .availabilityZoneId(specification.availabilityZoneId())
              .ebsOptimized(specification.ebsOptimized())
              .priority(specification.priority())
              .weight(specification.weight())
              .build());
    });

    builder.capacityReservationFleetId(crFleet.capacityReservationFleetId())
            .allocationStrategy(crFleet.allocationStrategy())
            .tenancy(crFleet.tenancyAsString())
            .totalTargetCapacity(crFleet.totalTargetCapacity())
            .instanceTypeSpecifications(instanceTypeSpecifications)
            .tagSpecifications(CollectionUtils.isEmpty(tagSpecifications) ? null : tagSpecifications)
            .instanceMatchCriteria(crFleet.instanceMatchCriteria().toString())
            .endDate(crFleet.endDate() != null ? String.valueOf(crFleet.endDate().getEpochSecond()) : null)
            .build();

    final ResourceModel model = builder.build();
    logger.log("[INFO] Mapped ResourceModel: " + model);

    return model;
  }

  public static ProgressEvent<ResourceModel, software.amazon.ec2.capacityreservationfleet.CallbackContext> translateToResourceFoundProgress(
          final DescribeCapacityReservationFleetsResponse response,
          final Logger logger,
          final software.amazon.ec2.capacityreservationfleet.CallbackContext context,
          final ResourceModel model) {
    try {
      validateReadResponse(response, logger);
      return ProgressEvent.defaultInProgressHandler(context, 0, model);
    } catch (final BaseHandlerException ex) {
      return ProgressEvent.defaultFailureHandler(ex, ex.getErrorCode());
    }
  }

  /**
   * Request to delete a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  public static CancelCapacityReservationFleetsRequest translateToDeleteRequest(final ResourceModel model, final Logger logger) {
    logger.log(String.format("[INFO] translateToDeleteRequest : %s", model));
    return CancelCapacityReservationFleetsRequest.builder().capacityReservationFleetIds(model.getCapacityReservationFleetId()).build();
  }

  /**
   * Request to update properties of a previously created resource
   *
   * https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
   * The input to an update handler MUST be valid against the resource schema.
   * Any createOnlyProperties specified in update handler input MUST NOT be different from their previous state.
   * The input to an update handler MUST contain either the primaryIdentifier or an additionalIdentifier.
   *
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  public static ModifyCapacityReservationFleetRequest translateToUpdateRequest(final ResourceModel model, final Logger logger) {
    final String crFleetId = model.getCapacityReservationFleetId();
    final ModifyCapacityReservationFleetRequest.Builder builder = ModifyCapacityReservationFleetRequest.builder();
    builder.capacityReservationFleetId(crFleetId);

    if (model.getTotalTargetCapacity() != null) {
      builder.totalTargetCapacity(model.getTotalTargetCapacity());
    }

    if (model.getNoRemoveEndDate() != null && model.getRemoveEndDate() != null) {
      logger.log(String.format("[INFO] caller specified both NoRemoveEndDate and RemoveEndDate in modifyCapacityReservationFleet request. crFleetId: %s", crFleetId));
      throw new CfnInvalidRequestException("ModifyCapacityReservationFleet request cannot have both NoRemoveEndDate and RemoveEndDate.");
    } else if(model.getRemoveEndDate() != null) {
      builder.removeEndDate(model.getRemoveEndDate());
    } else if(model.getNoRemoveEndDate() != null && !StringUtils.isBlank(model.getEndDate())) {
      builder.endDate(new Date(Integer.parseInt(model.getEndDate())).toInstant());
    }

    return builder.build();
  }

  /**
   * Request to list resources
   *
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  public static DescribeCapacityReservationFleetsRequest translateToListRequest(final String nextToken) {
    return DescribeCapacityReservationFleetsRequest.builder().nextToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * Return only SUCCESS for FAILED status as per the official guide - https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
   *
   * @param response the aws service describe resource response
   * @return list of resource models
   */
  public static List<ResourceModel> translateFromListRequest(final DescribeCapacityReservationFleetsResponse response) {
    return streamOfOrEmpty(response.capacityReservationFleets())
            .filter(fleet ->
                    fleet.state() != null &&
                            (fleet.state().equals(CapacityReservationFleetState.ACTIVE) ||
                            fleet.state().equals(CapacityReservationFleetState.PARTIALLY_FULFILLED) ||
                            fleet.state().equals(CapacityReservationFleetState.FAILED)))
            .map(fleet -> ResourceModel.builder()
                    .capacityReservationFleetId(fleet.capacityReservationFleetId())
                    .build())
            .collect(Collectors.toList());
  }

  /**
   * inject tag sepcifications to request object.
   *
   * @param handlerRequest
   * @param model
   * @return
   */
  private static List<software.amazon.awssdk.services.ec2.model.TagSpecification> getTags(
          final ResourceHandlerRequest<ResourceModel> handlerRequest, final ResourceModel model, final Logger logger) {
    final List<software.amazon.awssdk.services.ec2.model.Tag> tags = new ArrayList<>();
    final List<software.amazon.awssdk.services.ec2.model.TagSpecification> fleetTagSpecifications = new ArrayList<>();

    // Get stack-level tags from CFN
    if (handlerRequest.getDesiredResourceTags() != null) {
      handlerRequest.getDesiredResourceTags().forEach((key, value) -> {
        software.amazon.awssdk.services.ec2.model.Tag tag = software.amazon.awssdk.services.ec2.model.Tag.builder()
                .key(key)
                .value(value)
                .build();
        tags.add(tag);
      });
    }

    /* Get CFN system tags https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-resource-tags.html
      aws:cloudformation:logical-id
      aws:cloudformation:stack-id
      aws:cloudformation:stack-name
     */
    if (handlerRequest.getSystemTags() != null) {
      handlerRequest.getSystemTags().forEach((key, value) -> {
        software.amazon.awssdk.services.ec2.model.Tag tag = software.amazon.awssdk.services.ec2.model.Tag.builder()
                .key(key)
                .value(value)
                .build();
        tags.add(tag);
      });
    }

    if (tags.isEmpty()) {
      logger.log("No stack-level tags and system tags for CFN");
    } else {
      logger.log("CFN stack-level and system tags : " + tags);
      fleetTagSpecifications.add(software.amazon.awssdk.services.ec2.model.TagSpecification.builder().resourceType(CR_FLEET_TAG_RESOURCE_TYPE).tags(tags).build());
    }

    // Get user-provided tags
    if (model.getTagSpecifications() != null && model.getTagSpecifications().size() > 0) {
      fleetTagSpecifications.addAll(model.getTagSpecifications().stream().map(spec -> software.amazon.awssdk.services.ec2.model.TagSpecification.builder().resourceType(spec.getResourceType()).tags(
              spec.getTags().stream().map(tag -> software.amazon.awssdk.services.ec2.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                      .collect(Collectors.toList())).build()).collect(Collectors.toList()));
    }

    logger.log("TagSpecifications to add : " + fleetTagSpecifications);

    return fleetTagSpecifications;
  }

  private static ProgressEvent<ResourceModel, software.amazon.ec2.capacityreservationfleet.CallbackContext> translateServiceExceptionToFailure(final AwsServiceException ex) {
    if (ex.isThrottlingException()) {
      return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.Throttling);
    } else if (500 <= ex.statusCode()) {
      return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.ServiceInternalError);
    } else if (ex.awsErrorDetails() != null) {
      final String errorCode = ex.awsErrorDetails().errorCode();
      if (errorCode.equalsIgnoreCase(INVALID_CR_FLEET_ID_MALFORMED) || errorCode.equalsIgnoreCase(INVALID_CR_FLEET_ID_NOT_FOUND)) {
        return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.NotFound);
      } else if (errorCode.equalsIgnoreCase(INVALID_CR_FLEET_STATE_TRANSITION)) { // when customer tries to modify a cr fleet in unstable status (i.e.,modifying)
        return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.NotStabilized);
      } else if (errorCode.equalsIgnoreCase(UNAUTHORIZED_CODE)) {
        return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.InvalidRequest);
      }
    }

    return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.GeneralServiceException);
  }

  private static ProgressEvent<ResourceModel, software.amazon.ec2.capacityreservationfleet.CallbackContext> translateSdkExceptionToFailure(final SdkException ex) {
    if (ex instanceof AwsServiceException) {
      return translateServiceExceptionToFailure((AwsServiceException) ex);
    }

    return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.GeneralServiceException);
  }

  public static ProgressEvent<ResourceModel, CallbackContext> translateToFailure(final Exception ex) {
    if (ex instanceof SdkException) {
      return translateSdkExceptionToFailure((SdkException)ex);
    }

    return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.ServiceInternalError);
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  /**
   *  Validate a response based on the rules defined the CFN AWS public documentation:
   *
   * A read handler MUST always return a status of SUCCESS or FAILED; it MUST NOT return a status of IN_PROGRESS.
   * A read handler MUST return FAILED with a NotFound error code if the resource does not exist.
   *
   * @param response
   * @param logger
   */
  private static void validateReadResponse(
          final DescribeCapacityReservationFleetsResponse response,
          final Logger logger) {
    logger.log(String.format("[INFO] Validating DescribeCapacityReservationFleetsResponse: %s", response));

    if (response == null || !response.hasCapacityReservationFleets() || (response.capacityReservationFleets() == null || response.capacityReservationFleets().size() == 0)) {
      final String requestId = response != null && response.responseMetadata() != null ?
              response.responseMetadata().requestId() : null;
      final String message = String.format("Failed to describe capacity reservation fleet. DescribeCapacityReservationFleets requestId: %s", requestId);
      logger.log(String.format("[WARN] %s", message));

      throw new CfnServiceInternalErrorException(message);
    }

    response.capacityReservationFleets().stream().forEach(fleet -> {
      logger.log(String.format("[INFO] checking fleet status: %s", fleet.state()));
      if (CapacityReservationFleetState.FAILED.equals(fleet.state()) ||
              CapacityReservationFleetState.CANCELLED.equals(fleet.state()) ||
              CapacityReservationFleetState.EXPIRED.equals(fleet.state())) {
        logger.log(String.format("[INFO] CRFleet %s is not in an active state.", fleet.capacityReservationFleetId()));
        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, fleet.capacityReservationFleetId());
      } else if (CapacityReservationFleetState.SUBMITTED.equals(fleet.state()) ||
              CapacityReservationFleetState.CANCELLING.equals(fleet.state()) ||
              CapacityReservationFleetState.EXPIRING.equals(fleet.state()) ||
              CapacityReservationFleetState.MODIFYING.equals(fleet.state())) {
        logger.log(String.format("[INFO] CRFleet %s is in a in_progress state. state: %s. Throwing NotStabilizedException.",
                fleet.capacityReservationFleetId(), fleet.state()));

        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, fleet.capacityReservationFleetId());
      }
    });

    logger.log(String.format("[INFO] Validation done for DescribeCapacityReservationFleetsResponse: %s", response));
  }
}

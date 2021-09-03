# AWS::EC2::CapacityReservationFleet

Resource Type definition for AWS::EC2::CapacityReservationFleet

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::EC2::CapacityReservationFleet",
    "Properties" : {
        "<a href="#allocationstrategy" title="AllocationStrategy">AllocationStrategy</a>" : <i>String</i>,
        "<a href="#tagspecifications" title="TagSpecifications">TagSpecifications</a>" : <i>[ <a href="tagspecification.md">TagSpecification</a>, ... ]</i>,
        "<a href="#instancetypespecifications" title="InstanceTypeSpecifications">InstanceTypeSpecifications</a>" : <i>[ <a href="instancetypespecification.md">InstanceTypeSpecification</a>, ... ]</i>,
        "<a href="#totaltargetcapacity" title="TotalTargetCapacity">TotalTargetCapacity</a>" : <i>Integer</i>,
        "<a href="#enddate" title="EndDate">EndDate</a>" : <i>String</i>,
        "<a href="#instancematchcriteria" title="InstanceMatchCriteria">InstanceMatchCriteria</a>" : <i>String</i>,
        "<a href="#tenancy" title="Tenancy">Tenancy</a>" : <i>String</i>,
        "<a href="#removeenddate" title="RemoveEndDate">RemoveEndDate</a>" : <i>Boolean</i>,
        "<a href="#noremoveenddate" title="NoRemoveEndDate">NoRemoveEndDate</a>" : <i>Boolean</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::EC2::CapacityReservationFleet
Properties:
    <a href="#allocationstrategy" title="AllocationStrategy">AllocationStrategy</a>: <i>String</i>
    <a href="#tagspecifications" title="TagSpecifications">TagSpecifications</a>: <i>
      - <a href="tagspecification.md">TagSpecification</a></i>
    <a href="#instancetypespecifications" title="InstanceTypeSpecifications">InstanceTypeSpecifications</a>: <i>
      - <a href="instancetypespecification.md">InstanceTypeSpecification</a></i>
    <a href="#totaltargetcapacity" title="TotalTargetCapacity">TotalTargetCapacity</a>: <i>Integer</i>
    <a href="#enddate" title="EndDate">EndDate</a>: <i>String</i>
    <a href="#instancematchcriteria" title="InstanceMatchCriteria">InstanceMatchCriteria</a>: <i>String</i>
    <a href="#tenancy" title="Tenancy">Tenancy</a>: <i>String</i>
    <a href="#removeenddate" title="RemoveEndDate">RemoveEndDate</a>: <i>Boolean</i>
    <a href="#noremoveenddate" title="NoRemoveEndDate">NoRemoveEndDate</a>: <i>Boolean</i>
</pre>

## Properties

#### AllocationStrategy

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### TagSpecifications

_Required_: No

_Type_: List of <a href="tagspecification.md">TagSpecification</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### InstanceTypeSpecifications

_Required_: Yes

_Type_: List of <a href="instancetypespecification.md">InstanceTypeSpecification</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### TotalTargetCapacity

_Required_: Yes

_Type_: Integer

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### EndDate

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### InstanceMatchCriteria

_Required_: No

_Type_: String

_Allowed Values_: <code>open</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tenancy

_Required_: No

_Type_: String

_Allowed Values_: <code>default</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### RemoveEndDate

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NoRemoveEndDate

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the CapacityReservationFleetId.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### CapacityReservationFleetId

Returns the <code>CapacityReservationFleetId</code> value.


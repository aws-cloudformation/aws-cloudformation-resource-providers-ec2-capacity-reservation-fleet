# AWS::EC2::CapacityReservationFleet TagSpecification

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#resourcetype" title="ResourceType">ResourceType</a>" : <i>String</i>,
    "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#resourcetype" title="ResourceType">ResourceType</a>: <i>String</i>
<a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
</pre>

## Properties

#### ResourceType

_Required_: No

_Type_: String

_Allowed Values_: <code>client-vpn-endpoint</code> | <code>customer-gateway</code> | <code>dedicated-host</code> | <code>dhcp-options</code> | <code>egress-only-internet-gateway</code> | <code>elastic-gpu</code> | <code>elastic-ip</code> | <code>export-image-task</code> | <code>export-instance-task</code> | <code>fleet</code> | <code>capacity-reservation</code> | <code>capacity-reservation-fleet</code> | <code>fpga-image</code> | <code>host-reservation</code> | <code>image</code> | <code>import-image-task</code> | <code>import-snapshot-task</code> | <code>instance</code> | <code>internet-gateway</code> | <code>key-pair</code> | <code>launch-template</code> | <code>local-gateway-route-table-vpc-association</code> | <code>natgateway</code> | <code>network-acl</code> | <code>network-insights-analysis</code> | <code>network-insights-path</code> | <code>network-interface</code> | <code>placement-group</code> | <code>reserved-instances</code> | <code>route-table</code> | <code>security-group</code> | <code>snapshot</code> | <code>spot-fleet-request</code> | <code>spot-instances-request</code> | <code>subnet</code> | <code>traffic-mirror-filter</code> | <code>traffic-mirror-session</code> | <code>traffic-mirror-target</code> | <code>transit-gateway</code> | <code>transit-gateway-attachment</code> | <code>transit-gateway-connect-peer</code> | <code>transit-gateway-multicast-domain</code> | <code>transit-gateway-route-table</code> | <code>volume</code> | <code>vpc</code> | <code>vpc-flow-log</code> | <code>vpc-peering-connection</code> | <code>vpn-connection</code> | <code>vpn-gateway</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)


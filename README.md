## AWS CloudFormation Resource Provider Package For EC2 Capacity Reservation Fleet 

This repository contains AWS-owned resource providers for the AWS::EC2CapacityReservationFleet::* namespace.

### Usage
The CloudFormation CLI (cfn) allows you to author your own resource providers that can be used by CloudFormation.

Refer to the documentation for the [CloudFormation CLI](https://github.com/aws-cloudformation/cloudformation-cli) for usage instructions.

### Development
First, you will need to install the CloudFormation CLI, as it is a required dependency:

```
pip3 install cloudformation-cli
pip3 install cloudformation-cli-java-plugin
```
Linting and running unit tests is done via [pre-commit](https://pre-commit.com/), and so is performed automatically on commit. The continuous integration also runs these checks.

```pre-commit install```

Manual options are available so you don't have to commit:

```
# run all hooks on all files, mirrors what the CI runs
pre-commit run --all-files
# run unit tests and coverage checks
mvn verify
```

### More Resources
- [Tutorial - Create a State Machine using AWS CloudFormation](https://docs.aws.amazon.com/step-functions/latest/dg/tutorial-lambda-state-machine-cloudformation.html)
- [Code of Conduct](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-ec2-capacity-reservation-fleet/blob/main/CODE_OF_CONDUCT.md)
- [Contributing Guide](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-ec2-capacity-reservation-fleet/blob/main/CONTRIBUTING.md)
- [License](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-ec2-capacity-reservation-fleet/blob/main/LICENSE.md)

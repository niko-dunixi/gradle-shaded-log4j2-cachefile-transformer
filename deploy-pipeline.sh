#!/usr/bin/env bash
set -e
source sourcable-aws-variables.sh
aws cloudformation create-stack --stack-name "${PIPELINE_NAME}" --template-body=file://./cloudformation-template.yml --capabilities CAPABILITY_NAMED_IAM

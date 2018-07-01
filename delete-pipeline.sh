#!/usr/bin/env bash
set -e
source sourcable-aws-variables.sh
echo "Deleting CloudFormation stack"
aws cloudformation delete-stack --stack-name "${PIPELINE_NAME}"
echo "Delete succeeded? ${?}"

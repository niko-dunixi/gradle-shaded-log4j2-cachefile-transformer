#!/usr/bin/env bash
# A convienience file that lets me set these variables for a given
# terminal session. If you're not in the know, run "source sourcable-aws-variables.sh"
# to set these variables for your current session.

# Set the variables
AWS_REGION=us-west-2
AWS_PROFILE=paulbaker
PIPELINE_NAME=gradle-shaded-log4j-transformer
# Export them so the terminal you're currently using has access to these values.
export AWS_REGION
export AWS_PROFILE
export PIPELINE_NAME
echo "Setting AWS_PROFILE to ${AWS_PROFILE} and AWS_REGION to ${AWS_REGION} for ${PIPELINE_NAME}"

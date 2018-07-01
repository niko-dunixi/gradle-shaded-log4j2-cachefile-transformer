#!/usr/bin/env bash
set -e
source sourcable-aws-variables.sh
aws codepipeline start-pipeline-execution --name "${PIPELINE_NAME}-pipeline"

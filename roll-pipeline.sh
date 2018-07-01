#!/usr/bin/env bash
set -e
./delete-pipeline.sh
sleep 5s
./deploy-pipeline.sh

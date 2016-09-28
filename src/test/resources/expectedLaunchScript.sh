#!/usr/bin/env sh

set -x
# set session
set -m

# Set Environment Variables.
ARTIFACT_FILE_NAME=artifact.war
export ARTIFACT_FILE_NAME
WINGS_BACKUP_PATH=/tmp/backup
export WINGS_BACKUP_PATH
PORT=8080
export PORT
WINGS_RUNTIME_PATH=/tmp/runtime
export WINGS_RUNTIME_PATH
WINGS_SCRIPT_DIR=/tmp/ACTIVITY_ID
export WINGS_SCRIPT_DIR
WINGS_STAGING_PATH=/tmp/staging
export WINGS_STAGING_PATH

if [ "$#" -gt 1 ]
then
  key="$1"
  case $key in
    -w)
    shift # past argument
    eval WINGS_SCRIPT_WORKING_DIRECTORY=$1
    cd "$WINGS_SCRIPT_WORKING_DIRECTORY"
    shift
    ;;
    *)
    ;;
  esac
fi

WINGS_SCRIPT_NAME=$1
shift

$WINGS_SCRIPT_DIR/$WINGS_SCRIPT_NAME "$@"

#!/usr/bin/env bash

yq delete -i /opt/harness/verification-config.yml server.adminConnectors
yq delete -i /opt/harness/verification-config.yml server.applicationConnectors[0]

if [[ "" != "$LOGGING_LEVEL" ]]; then
  yq write -i /opt/harness/verification-config.yml logging.level "$LOGGING_LEVEL"
fi

if [[ "" != "$VERIFICATION_PORT" ]]; then
  yq write -i /opt/harness/verification-config.yml server.applicationConnectors[0].port "$VERIFICATION_PORT"
else
  yq write -i /opt/harness/verification-config.yml server.applicationConnectors[0].port "7070"
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i /opt/harness/verification-config.yml mongo.uri "${MONGO_URI//\\&/&}"
fi


if [[ "" != "$MANAGER_URL" ]]; then
  yq write -i /opt/harness/verification-config.yml managerUrl "$MANAGER_URL"
fi

  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[0].type "console"
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[0].threshold "TRACE"
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[0].target "STDOUT"

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[2]
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  if [[ "$ROLLING_FILE_LOGGING_ENABLED" == "true" ]]; then
    yq delete -i $CONFIG_FILE logging.appenders[1]
    yq write -i $CONFIG_FILE logging.appenders[1].currentLogFilename "/opt/harness/logs/verification.log"
    yq write -i $CONFIG_FILE logging.appenders[1].archivedLogFilenamePattern "/opt/harness/logs/verification.%d.%i.log"
  else
    yq delete -i $CONFIG_FILE logging.appenders[2]
    yq delete -i $CONFIG_FILE logging.appenders[1]
  fi
fi

if [[ "" != "$DATA_STORE" ]]; then
  yq write -i /opt/harness/verification-config.yml dataStorageMode "$DATA_STORE"
fi

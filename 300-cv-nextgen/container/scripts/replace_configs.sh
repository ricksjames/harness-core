#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

CONFIG_FILE=/opt/harness/cv-nextgen-config.yml
REDISSON_CACHE_FILE=/opt/harness/redisson-jcache.yaml

replace_key_value () {
  CONFIG_KEY="$1";
  CONFIG_VALUE="$2";
  if [[ "" != "$CONFIG_VALUE" ]]; then
    yq -i '.$CONFIG_KEY = $CONFIG_VALUE' $CONFIG_FILE
  fi
}

yq -i 'del(.server.adminConnectors)' /opt/harness/cv-nextgen-config.yml
yq -i 'del(.server.applicationConnectors[0])' /opt/harness/cv-nextgen-config.yml
yq -i 'del(.pmsSdkGrpcServerConfig.connectors[0])' $CONFIG_FILE

if [[ "" != "$LOGGING_LEVEL" ]]; then
  yq -i '.logging.level = "$LOGGING_LEVEL"' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$VERIFICATION_PORT" ]]; then
  yq -i '.server.applicationConnectors[0].port = "$VERIFICATION_PORT"' /opt/harness/cv-nextgen-config.yml
else
  yq -i '.server.applicationConnectors[0].port = "6060"' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq -i '.mongo.uri = "${MONGO_URI//\\&/&}"' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$MANAGER_CLIENT_BASEURL" ]]; then
  yq -i '.managerClientConfig.baseUrl = "$MANAGER_CLIENT_BASEURL"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_URL" ]]; then
  yq -i '.nextGen.ngManagerUrl = "$NG_MANAGER_URL"' $CONFIG_FILE
fi

  yq -i '.server.requestLog.appenders[0].type = "console"' /opt/harness/cv-nextgen-config.yml
  yq -i '.server.requestLog.appenders[0].threshold = "TRACE"' /opt/harness/cv-nextgen-config.yml
  yq -i '.server.requestLog.appenders[0].target = "STDOUT"' /opt/harness/cv-nextgen-config.yml

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq -i 'del(.logging.appenders[0])' /opt/harness/cv-nextgen-config.yml
  yq -i '.logging.appenders[0].stackdriverLogEnabled = "true"' /opt/harness/cv-nextgen-config.yml
else
  yq -i 'del(.logging.appenders[1])' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$DATA_STORE" ]]; then
  yq -i '.dataStorageMode = "$DATA_STORE"' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$NEXT_GEN_MANAGER_SECRET" ]]; then
  yq -i '.nextGen.managerServiceSecret = "$NEXT_GEN_MANAGER_SECRET"' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$MANAGER_JWT_AUTH_SECRET" ]]; then
  yq -i '.managerAuthConfig.jwtAuthSecret = "$MANAGER_JWT_AUTH_SECRET"' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$JWT_IDENTITY_SERVICE_SECRET" ]]; then
  yq -i '.managerAuthConfig.jwtIdentityServiceSecret = "$JWT_IDENTITY_SERVICE_SECRET"' /opt/harness/cv-nextgen-config.yml
fi

if [[ "" != "$MONGO_INDEX_MANAGER_MODE" ]]; then
  yq -i '.mongo.indexManagerMode = $MONGO_INDEX_MANAGER_MODE' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_URL" ]]; then
  yq -i '.nextGen.ngManagerUrl = "$NG_MANAGER_URL"' $CONFIG_FILE
fi

if [[ "" != "$NOTIFICATION_BASE_URL" ]]; then
  yq -i '.notificationClient.httpClient.baseUrl = "$NOTIFICATION_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$NOTIFICATION_MONGO_URI" ]]; then
  yq -i '.notificationClient.messageBroker.uri = "${NOTIFICATION_MONGO_URI//\\&/&}"' $CONFIG_FILE
fi

if [[ "" != "$PORTAL_URL" ]]; then
  yq -i '.portalUrl = "$PORTAL_URL"' $CONFIG_FILE
fi

replace_key_value eventsFramework.redis.sentinel $EVENTS_FRAMEWORK_USE_SENTINEL
replace_key_value eventsFramework.redis.envNamespace $EVENTS_FRAMEWORK_ENV_NAMESPACE
replace_key_value eventsFramework.redis.redisUrl $EVENTS_FRAMEWORK_REDIS_URL
replace_key_value eventsFramework.redis.masterName $EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME
replace_key_value eventsFramework.redis.userName $EVENTS_FRAMEWORK_REDIS_USERNAME
replace_key_value eventsFramework.redis.password $EVENTS_FRAMEWORK_REDIS_PASSWORD
replace_key_value eventsFramework.redis.sslConfig.enabled $EVENTS_FRAMEWORK_REDIS_SSL_ENABLED
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePath $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PATH
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePassword $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PASSWORD

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_FRAMEWORK_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq -i '.eventsFramework.redis.sentinelUrls.[$INDEX] = "${REDIS_SENTINEL_URL}"' $CONFIG_FILE
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$PMS_TARGET" ]]; then
  yq -i '.pmsGrpcClientConfig.target = $PMS_TARGET' $CONFIG_FILE
fi

if [[ "" != "$PMS_AUTHORITY" ]]; then
  yq -i '.pmsGrpcClientConfig.authority = $PMS_AUTHORITY' $CONFIG_FILE
fi

if [[ "" != "$SHOULD_CONFIGURE_WITH_PMS" ]]; then
  yq -i '.shouldConfigureWithPMS = $SHOULD_CONFIGURE_WITH_PMS' $CONFIG_FILE
fi

if [[ "" != "$GRPC_SERVER_PORT" ]]; then
  yq -i '.pmsSdkGrpcServerConfig.connectors[0].port = "$GRPC_SERVER_PORT"' $CONFIG_FILE
fi

yq -i 'del(.codec)' $REDISSON_CACHE_FILE

if [[ "$REDIS_SCRIPT_CACHE" == "false" ]]; then
  yq -i '.useScriptCache = false' $REDISSON_CACHE_FILE
fi


if [[ "" != "$CACHE_CONFIG_REDIS_URL" ]]; then
  yq -i '.singleServerConfig.address = "$CACHE_CONFIG_REDIS_URL"' $REDISSON_CACHE_FILE
fi

if [[ "$CACHE_CONFIG_USE_SENTINEL" == "true" ]]; then
  yq -i 'del(.singleServerConfig)' $REDISSON_CACHE_FILE
fi

if [[ "" != "$CACHE_CONFIG_SENTINEL_MASTER_NAME" ]]; then
  yq -i '.sentinelServersConfig.masterName = "$CACHE_CONFIG_SENTINEL_MASTER_NAME"' $REDISSON_CACHE_FILE
fi

if [[ "" != "$CACHE_CONFIG_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$CACHE_CONFIG_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq -i '.sentinelServersConfig.sentinelAddresses.[+] = "${REDIS_SENTINEL_URL}"' $REDISSON_CACHE_FILE
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$REDIS_NETTY_THREADS" ]]; then
  yq -i '.nettyThreads = "$REDIS_NETTY_THREADS"' $REDISSON_CACHE_FILE
fi

replace_key_value cacheConfig.cacheNamespace $CACHE_NAMESPACE
replace_key_value cacheConfig.cacheBackend $CACHE_BACKEND

replace_key_value cfClientConfig.apiKey "$CF_CLIENT_API_KEY"
replace_key_value cfClientConfig.configUrl "$CF_CLIENT_CONFIG_URL"
replace_key_value cfClientConfig.eventUrl "$CF_CLIENT_EVENT_URL"
replace_key_value cfClientConfig.analyticsEnabled "$CF_CLIENT_ANALYTICS_ENABLED"
replace_key_value cfClientConfig.connectionTimeout "$CF_CLIENT_CONNECTION_TIMEOUT"
replace_key_value cfClientConfig.readTimeout "$CF_CLIENT_READ_TIMEOUT"
replace_key_value featureFlagConfig.featureFlagSystem "$FEATURE_FLAG_SYSTEM"
replace_key_value featureFlagConfig.syncFeaturesToCF "$SYNC_FEATURES_TO_CF"
replace_key_value notificationClient.secrets.notificationClientSecret "$PLATFORM_SERVICE_SECRET"

replace_key_value accessControlClientConfig.enableAccessControl "$ACCESS_CONTROL_ENABLED"
replace_key_value accessControlClientConfig.accessControlServiceConfig.baseUrl "$ACCESS_CONTROL_BASE_URL"
replace_key_value accessControlClientConfig.accessControlServiceSecret "$ACCESS_CONTROL_SECRET"

replace_key_value templateServiceClientConfig.baseUrl "$TEMPLATE_SERVICE_ENDPOINT"

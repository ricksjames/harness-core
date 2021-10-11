#!/usr/bin/env bash


declare -a MODULES_SKIPPED=("001-microservice-intfc-tool" "160-model-gen-tool" "190-deployment-functional-tests" "200-functional-test" "220-graphql-test" "230-model-test" "990-commons-test")

for MODULE in `find . -iname build.bazel | cut -f 2 -d "/" | sort | uniq | grep [0-9]`
  do
  	skipped=0
  	for MODULE_SKIPPED in "${MODULES_SKIPPED[@]}"
  	do
  	  if [ "$MODULE_SKIPPED" = "$MODULE" ]; then
  	  	skipped=1
  	  fi
  	done
  	if [ "$skipped" = 1 ]; then
  	  continue;
  	fi
  	n=0
  	for DEP_OF_TOOL in `cat 001-microservice-intfc-tool/BUILD.bazel | grep [0-9] | cut -f 2 -d "\"" | cut -f 3 -d "/" | cut -f 1 -d ":" | sort | uniq`
  	  do
  	  	if [ "$DEP_OF_TOOL" = "$MODULE" ]; then
  	  	  n=1
  	  	fi
  	  done
  	if [ "$n" = 0 ]; then
  	  echo "$MODULE not in dependency of microservice interface tool"
  	  exit 1
  	fi
  done
exit 0
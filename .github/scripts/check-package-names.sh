#!/bin/bash -e

# shellcheck disable=SC2001

for dir in $(find instrumentation -name "*.java" | grep library/src/main/java | sed 's#/[^/]*$##' | sort -u); do

  module_name=$(echo "$dir" | sed 's#.*/\([^/]*\)/library/src/main/java/.*#\1#')

  if [[ "$module_name" =~ java-* ]]; then
    continue
  fi
  if [[ "$module_name" == "jdbc" ]]; then
    continue
  fi
  if [[ "$module_name" == "jmx-metrics" ]]; then
    continue
  fi
  if [[ "$module_name" == "resources" ]]; then
    continue
  fi
  if [[ "$module_name" == "oshi" ]]; then
    continue
  fi

  # these are possibly problematic
  if [[ "$dir" == "instrumentation/grpc-1.6/library/src/main/java/io/grpc/override" ]]; then
    continue
  fi
  if [[ "$dir" == "instrumentation/lettuce/lettuce-5.1/library/src/main/java/io/lettuce/core/protocol" ]]; then
    continue
  fi

  # some common modules don't have any base version
  # - lettuce-common
  # - netty-common
  if [[ ! "$module_name" =~ [0-9]$ && "$module_name" != "lettuce-common" && "$module_name" != "netty-common" ]]; then
    echo "module name doesn't have a base version: $dir"
    exit 1
  fi

  simple_module_name=$(echo "$module_name" | sed 's/-[0-9.]*$//' | sed 's/-//g')
  base_version=$(echo "$module_name" | sed 's/.*-\([0-9.]*\)$/\1/' | sed 's/\./_/')

  if [[ ! "$module_name" =~ [0-9]$ && "$module_name" != "lettuce-common" && "$module_name" != "netty-common" ]]; then
    expected_package_name="io/opentelemetry/instrumentation/$simple_module_name/v$base_version"
  else
    expected_package_name="io/opentelemetry/instrumentation/$simple_module_name"
  fi

  package_name=$(echo "$dir" | sed 's#.*/src/main/java/##')

  # deal with differences like module name elasticsearch-rest and package name elasticsearch.rest
  expected_package_name_normalized=$(echo "$expected_package_name" | sed 's#/##g')
  package_name_normalized=$(echo "$package_name" | sed 's#/##g')

  if [[ "$package_name_normalized" != "$expected_package_name_normalized"* ]]; then
    echo "ERROR: $dir"
    exit 1
  fi

done

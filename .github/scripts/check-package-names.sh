#!/bin/bash -e

# shellcheck disable=SC2001

# find instrumentation -type d -name "*-common*"

# TODO javaagent modules?
#for file in $(find instrumentation/aws-sdk -name "*.java" | grep library/src/main/java | sed 's#/[^/]*$##' | sort -u); do
for dir in $(cat out); do

  module_name=$(echo "$dir" | sed 's#.*/\([^/]*\)/library/src/main/java/.*#\1#')

  if [[ ! "$module_name" =~ [0-9]$ ]]; then
    echo "module name doesn't have a base version: $dir"
    exit 1
  fi

  simple_module_name=$(echo "$module_name" | sed 's/-[0-9.]*$//' | sed 's/-//g')
  base_version=$(echo "$module_name" | sed 's/.*-\([0-9.]*\)$/\1/' | sed 's/\./_/')

  echo $base_version

  expected_package_name="io/opentelemetry/instrumentation/$simple_module_name/v$base_version"

  package_name=$(echo "$dir" | sed 's#.*/src/main/java/##')

  # deal with differences like module name elasticsearch-rest and package name elasticsearch.rest
  expected_package_name_normalized=$(echo "$expected_package_name" | sed 's#/##g')
  package_name_normalized=$(echo "$package_name" | sed 's#/##g')

  if [[ "$package_name_normalized" != "$expected_package_name_normalized"* ]]; then
    echo "ERROR: $dir"
    exit 1
  fi

done

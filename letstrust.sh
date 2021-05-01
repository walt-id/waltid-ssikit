#!/bin/bash
set -e

function header() {
  echo "letstrust-ssi-core wrapper script"
  echo
}

function build() {
  echo
  echo "Building Let's Trust build files..."

  if ./gradlew clean assemble; then
    echo
    echo "Build was successful."
    echo "Continuing with build file extraction..."
    echo
    extract
  else
    echo
    echo "Build was unsuccessful. Will not extract the build files."
    echo
  fi
}

function extract() {
  echo "Extracting Let's Trust build files..."

  echo

  if [[ ! -d build/distributions ]]; then
    echo "The build files do not exist (directory ./build/distributions)."
    echo "Have you run \"./letstrust.sh build\" yet?"
    echo
    exit
  fi

  if [[ ! -f build/distributions/letstrust-ssi-core-1.0-SNAPSHOT.tar ]]; then
    echo "The build files do not exist (directory ./build/distributions)."
    echo "Have you run \"./letstrust.sh build\" yet?"
    echo
    exit
  fi

  (
    cd build/distributions
    if tar xf letstrust-ssi-core-1.0-SNAPSHOT.tar; then
      echo "Extraction successful."
    else
      echo "Extracting was unsuccessful."
      echo
      exit
    fi
    echo
  )
}

function execute() {
  if [[ -f build/distributions/letstrust-ssi-core-1.0-SNAPSHOT/bin/letstrust-ssi-core ]]; then
    build/distributions/letstrust-ssi-core-1.0-SNAPSHOT/bin/letstrust-ssi-core "$@"
  else
    echo "Cannot run Let's Trust: Runscript does not exist."
  fi
}

function help() {
  echo "Usage: $0 {build|extract|execute (default)}"
  echo
  echo "Use \"execute\" to execute letstrust-ssi-core with no arguments. If you don't supply any"
  echo "arguments of {build|extract|execute}, letstrust-ssi-core will be executed with the provided arguments."
}

if [[ $# -eq 0 ]]; then
  header
  help
else
  case "$1" in
  build | rebuild)
    header
    build
    ;;
  extract)
    header
    extract
    ;;
  execute)
    shift
    execute "$@"
    ;;
  help)
    header
    help
    ;;
  *)
    execute "$@"
    ;;
  esac
fi

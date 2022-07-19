#!/bin/bash
set -e

version='1.11.0-SNAPSHOT'

function header() {
  echo "waltid-ssi-kit wrapper script"
  echo
}

function build() {
  echo "Building walt.id build files..."

  if ./gradlew clean build; then
    echo
    echo "Build was successful."
    echo "Continuing with build file extraction..."
    echo
    extract
  else
    echo
    echo "Build was unsuccessful. Will not extract the build files."
    exit
    echo
  fi
}

function build_skip_tests() {
  echo "Building walt.id build files..."

  if ./gradlew clean build -x test; then
    echo
    echo "Build was successful."
    echo "Continuing with build file extraction..."
    echo
    extract
  else
    echo
    echo "Build was unsuccessful. Will not extract the build files."
    exit
    echo
  fi
}

function build_docker() {
  if docker build -t ssikit .; then
    echo
    echo "Docker container build was successful."
    echo
  else
    echo
    echo "Docker container build was unsuccessful."
    echo
    exit
  fi
}

function build_podman() {
  if podman build -t ssikit .; then
    echo
    echo "Podman container build was successful."
    echo
  else
    echo
    echo "Podman container build was unsuccessful."
    echo
    exit
  fi
}

function extract() {
  echo "Extracting walt.id build files..."

  echo

  if [[ ! -d build/distributions ]]; then
    echo "The directory ./build/distributions does not exist."
    echo "Have you run \"./ssikit.sh build\" yet?"
    echo
    exit
  fi

  if [[ ! -f build/distributions/waltid-ssi-kit-$version.tar ]]; then
    echo "The build files do not exist (directory ./build/distributions)."
    echo "Have you run \"./ssikit.sh build\" yet?"
    echo
    exit
  fi

  (
    cd build/distributions
    if tar xf waltid-ssi-kit-$version.tar; then
      echo "Extraction successful."
    else
      echo "Extracting was unsuccessful."
      echo
      exit
    fi
    echo
  )
}

function build_runscript_question() {
  header
  echo "Cannot run walt.id: Runscript does not exist."
  echo "Have you built and extracted the buildfiles? ($0 build)"
  echo
  echo -n "Do you want to build ($0 build)? [y/n]: "
  read -r ans
}

function execute_debug() {
  if [[ -f build/distributions/waltid-ssi-kit-$version/bin/waltid-ssi-kit ]]; then
    JAVA_OPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG" build/distributions/waltid-ssi-kit-$version/bin/waltid-ssi-kit "$@"
  else
    build_runscript_question

    if [[ $ans != "n" ]]; then
      build_skip_tests
      execute_debug "$@"
    fi
  fi
}

function execute() {
  if [[ -f build/distributions/waltid-ssi-kit-$version/bin/waltid-ssi-kit ]]; then
    build/distributions/waltid-ssi-kit-$version/bin/waltid-ssi-kit "$@"
  else
    build_runscript_question

    if [[ $ans != "n" ]]; then
      build_skip_tests
      execute "$@"
    fi
  fi
}

function help() {
  echo "Usage: $0 {build|build-st|build-docker|build-podman|extract|--verbose|execute (default)}"
  echo
  echo "Use \"execute\" to execute waltid-ssi-kit with no arguments. If you don't supply any"
  echo "arguments of {build|build-st|build-docker|build-podman|extract}, waltid-ssi-kit will"
  echo "be executed with the provided arguments."
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
  build-st | rebuild-st)
    header
    build_skip_tests
    ;;
  build-docker | rebuild-docker)
    header
    build_docker
    ;;
  build-podman | rebuild-podman)
    header
    build_podman
    ;;
  extract)
    header
    extract
    ;;
  help)
    header
    help
    ;;
  -v | --verbose | -d | --debug)
    shift
    execute_debug "$@"
    ;;
  execute)
    shift
    execute "$@"
    ;;
  *)
    execute "$@"
    ;;
  esac
fi

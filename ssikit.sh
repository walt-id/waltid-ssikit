#!/bin/bash
set -e

function header() {
  echo "waltid-ssikit wrapper script"
  echo
}

function build() {
  echo "Building walt.id build files..."

  if ./gradlew clean build installDist; then
    echo
    echo "Build was successful."
    echo
  else
    echo
    echo "Build was unsuccessful."
    exit
    echo
  fi
}

function build_skip_tests() {
  echo "Building walt.id build files..."

  if ./gradlew clean installDist -x test; then
    echo
    echo "Build was successful."
    echo
  else
    echo
    echo "Build was unsuccessful."
    exit
    echo
  fi
}

function build_docker() {
  if docker build -t waltid/ssikit .; then
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
  if podman build -t waltid/ssikit .; then
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

function build_runscript_question() {
  header
  echo "Cannot run walt.id: Runscript does not exist."
  echo "Have you run 'build' yet? ($0 build)"
  echo
  echo -n "Do you want to build ($0 build)? [y/n]: "
  read -r ans
}

function execute_debug() {
  if [[ -f build/install/waltid-ssikit/bin/waltid-ssikit ]]; then
    JAVA_OPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG" build/install/waltid-ssikit/bin/waltid-ssikit "$@"
  else
    build_runscript_question

    if [[ $ans != "n" ]]; then
      build_skip_tests
      execute_debug "$@"
    fi
  fi
}

function execute() {
  if [[ -f build/install/waltid-ssikit/bin/waltid-ssikit ]]; then
    build/install/waltid-ssikit/bin/waltid-ssikit "$@"
  else
    build_runscript_question

    if [[ $ans != "n" ]]; then
      build_skip_tests
      execute "$@"
    fi
  fi
}

function clean() {
    ./gradlew clean
}

function help() {
  echo "Usage: $0 {build|build-st|build-docker|build-podman|extract|--verbose|execute (default)}"
  echo
  echo "Use \"execute\" to execute waltid-ssikit with no arguments. If you don't supply any"
  echo "arguments of {build|build-st|build-docker|build-podman|extract}, waltid-ssikit will"
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
  clean)
    header
    clean
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

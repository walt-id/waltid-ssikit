#!/bin/bash
set -e

function header() {
  echo "letstrust-ssi-core wrapper script"
  echo
}

function build() {
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
    exit
    echo
  fi
}

function build_docker() {
  build
  if docker build -t letstrust .; then
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
  build
  if podman build -t letstrust .; then
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
    echo "Have you built and extracted the buildfiles? ($0 build)"
    echo
    echo -n "Do you want to build ($0 build)? [y/n]: "
    read -r ans

    if [[ $ans != "n" ]]; then
      build
      execute "$@"
    fi
  fi
}

function help() {
  echo "Usage: $0 {build|build-docker|build-podman|extract|execute (default)}"
  echo
  echo "Use \"execute\" to execute letstrust-ssi-core with no arguments. If you don't supply any"
  echo "arguments of {build|build-docker|build-podman|extract|execute}, letstrust-ssi-core will"
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

# LetsTrust CLI

## Install

    docker pull letstrust/test
    docker tag  letstrust/test letstrust

## Build

    docker build -f docker/Dockerfile . -t letstrust

## Run

    docker run -it letstrust

_Examples_

    docker run -it letstrust key gen --algorithm Secp256k1

    docker run -it letstrust -c backend=api.letstrust.io -c timeout=3000 did register

    docker run -it letstrust vc verify vc.json

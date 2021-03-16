# LetsTrust CLI

## Install

    docker pull letstrust/test
    docker tag letstrust/test letstrust

## Build

    maven install
    docker build -f docker/Dockerfile . -t letstrust

## Push
    docker tag letstrust letstrust/test
    docker push letstrust/test

## Run
    docker run -it -v $(pwd)/data:/data letstrust

    java -jar target/letstrust-ssi-core-1.0-SNAPSHOT-jar-with-dependencies.jar

_For getting help, add "-h" to each command or sub-command e.g.:_

    docker run -it -v $(pwd)/data:/data letstrust did create -h

_For debug infos add "-v" e.g.:_
    
    docker run -it -v $(pwd)/data:/data letstrust -v did create

_Examples_ 

    docker run -it -v $(pwd)/data:/data letstrust key gen --algorithm Ed25519

    docker run -it -v $(pwd)/data:/data letstrust key list

    docker run -it -v $(pwd)/data:/data letstrust did create -m web

    docker run -it -v $(pwd)/data:/data letstrust did resolve --did did:web:mattr.global

    docker run -it -v $(pwd)/data:/data letstrust -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    docker run -it -v $(pwd)/data:/data letstrust vc verify data/vc/created/vc-1614291790088-default.json

    docker run -it -v $(pwd)/data:/data letstrust -v vc present data/vc/created/vc-1614291790088-default.json

    docker run -it -v $(pwd)/data:/data letstrust vc verify -p data/vc/presented/vp-1614291892489.json

## TODOs

- Add ConfigLoader https://github.com/sksamuel/hoplite

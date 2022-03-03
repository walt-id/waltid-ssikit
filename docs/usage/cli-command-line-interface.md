# CLI (Command line interface)

### For getting help, add "-h" to each command or sub-command e.g.:
    ./ssikit.sh did create -h
    or
    docker run -it -v $(pwd)/data:/app/data ssikit did create -h

### For debug infos add "-v" e.g.:

    ./ssikit.sh -v
    or
    docker run -it -v $(pwd)/data:/app/data ssikit -v did create

### Overwriting the default config:
    Simply add a file named _lestrust.yaml_ in the root folder and run ./ssikit.sh

    When using Docker, the following command will do the trick:
    docker run -it $(pwd)/data:/app/data -v $(pwd)/ssikit.yaml:/ssikit.yaml ssikit -v did create

### walt.id wrapper commands

    ./ssikit.sh key gen --algorithm Ed25519

    ./ssikit.sh key list

    ./ssikit.sh did create -m web

    ./ssikit.sh did resolve --did did:web:mattr.global

    ./ssikit.sh -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    ./ssikit.sh vc verify data/vc/created/vc-1614291790088-default.json

    ./ssikit.sh -v vc present data/vc/created/vc-1614291790088-default.json

    ./ssikit.sh vc verify -p data/vc/presented/vp-1614291892489.json

### walt.id Docker / Podman commands
    docker run -itv $(pwd)/data:/app/data ssikit key gen --algorithm Ed25519

    docker run -itv $(pwd)/data:/app/data ssikit key list

    docker run -itv $(pwd)/data:/app/data ssikit did create -m web

    docker run -itv $(pwd)/data:/app/data ssikit did resolve --did did:web:mattr.global

    docker run -itv $(pwd)/data:/app/data ssikit -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    docker run -itv $(pwd)/data:/app/data ssikit vc verify data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/app/data ssikit -v vc present data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/app/data ssikit vc verify -p data/vc/presented/vp-1614291892489.json

    docker run -itv $(pwd)/templates:/app/templates -v $(pwd)/data:/app/data ssikit vc templates list

    docker run -itv $(pwd)/data:/app/data -p 7000-7003:7000-7003 ssikit serve

    podman run -itv $(pwd)/data:/app/data -p 7000-7003:7000-7003 ssikit serve

### walt.id API service
To expose the API service using the CLI tool or the docker container, use one of the following commands:

**Show all options for specifying bind address and ports**

    ./ssikit.sh serve --help
    docker run -itv $(pwd)/data:/app/data -p 192.168.0.1:7000-7003:7000-7003 ssikit serve --help

**On localhost only using the default ports 7000-7003**

    ./ssikit.sh serve

**Binding on all network interfaces, using the default ports 7000-7003**

    ./ssikit.sh serve -b 0.0.0.0

    docker run -itv $(pwd)/data:/app/data -p 7000-7003:7000-7003 ssikit serve -b 0.0.0.0

**Binding on a specific network interface (e.g.: 192.168.0.1)**

    ./ssikit.sh serve -b 192.168.0.1

_Using docker one needs to bind to 0.0.0.0 in the container and limit the binding from outside using the docker run -p syntax like so:_

    docker run -itv $(pwd)/data:/app/data -p 192.168.0.1:7000-7003:7000-7003 ssikit serve -b 0.0.0.0

**Use custom ports by using the -p (Core API), -e (ESSIF API), -s (Signatory API) command options**

    ./ssikit.sh serve -p 8000 -e 8001 -s 8002

    docker run -itv $(pwd)/data:/app/data -p 8000-8002:8000-8002 ssikit serve -b 0.0.0.0 -p 8000 -e 8001 -s 8002


### EBSI DID Registration (via CLI)
Create a directory for the generated data (if not present already)

    mkdir -p data/ebsi
Paste your bearer token from https://app.preprod.ebsi.eu/users-onboarding in file *data/ebsi/bearer-token.txt*

    cat > data/ebsi/bearer-token.txt 

Use the **walt.id** command line tool for creating and registering the DID EBSI.

    "<cli-tool>" can be replaced with the startup-script "./ssikit.sh" (when running the code-base)

    e.g. ./ssikit.sh  key gen -a Secp256k1

    "<cli-tool>" can be replaced with  "docker run -itv $(pwd)/data:/app/data" (when running Docker)

    In case of Docker add: "-p 7000-7003:7000-7003" when the REST APIs are required.
    In case of Docker add: "-v $(pwd)/templates:/app/templates" when VC templates should be used.

    e.g. docker run -itv $(pwd)/data:/app/data -v $(pwd)/templates:/app/templates -p 7000-7003:7000-7003 ssikit vc -h


Create the DID controlling key and the ETH signing key. Note, that if a Secp256k1 DID controlling key is used, then the same key will be used for signing the ETH transaction automatically.

    <cli-tool>  key gen -a Secp256k1

Create the DID document

    <cli-tool> did -m ebsi -k <keyId>

Run the onboarding flow in order to receive the Verifiable Authentication, which is valid for 6 months

    <cli-tool> essif onboard -d <did-ebsi>

Run the auth-api flow for getting a short lived (15min) access token for write access to the ledger

    <cli-tool> essif auth-api -d <did-ebsi>

Register the DID on the ledger. Optionally the key for signing the ETH transaction can be specified (parameter *k*), if it is another key then the DID controlling key

    <cli-tool> essif did register -d <did-ebsi> 

Resolve DID EBSI from the command line or directly via the Swagger interface https://api.preprod.ebsi.eu/docs/?urls.primaryName=DID%20Registry%20API#/DID%20Registry/get-did-registry-v2-identifier

    <cli-tool> did resolve --did <did-ebsi> 

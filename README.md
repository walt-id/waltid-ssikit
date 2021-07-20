# LetsTrust SSI Core

SSI core services, with primarily focus on the European EBSI/ESSIF ecosystem.

The core services are in the scope of:
 - **Key Management** generation, import/export
 - **Decentralized Identifier (DID)** operations (register, update, deactivate)
 - **Verifiable Credential (VC)** operations (issue, present, verify)
 - **ESSIF/EBSI** related Use Cases (onboarding, VC exchange, etc.)


The **Kotlin/Java based library** can be directly integrated in Java apps. Alternatively the library or the additional **Docker container** can be run as RESTful webservice.

The **CLI tool** conveniently allows running all included functions manually. 

## :unlock: Usage

### LetsTrust Container

The simplest way of using _LetsTrust SSI Core_ library is by pulling the Docker Container an running it via **Docker** or **Podman**. 

Login to Container Registry:

    export CR_PAT=<token-read-packages>
    docker login

Pull & tag Container

    docker pull ghcr.io/letstrustid/letstrust:test
    docker tag ghcr.io/letstrustid/letstrust:test letstrust

Run via Docker:

    docker run -itv $(pwd)/data:/app/data letstrust -h

Run via Podman:

    mkdir data  # directory where the data is stored needs do be created manually
    podman run -itv $(pwd)/data:/app/data letstrust

Run as RESTful service via Docker Compose:

    docker-compose build
    docker-compose up


### LetsTrust Wrapper

Alternatively the LetsTrust wrapper script **letstrust.sh** is a convenient way for building and using the library on **Linux**. This requires [building](#hammer-build) (see below) the project and a Java 15 runtime environment.

    ./letstrust.sh {build|build-docker|build-podman|extract|execute (default)}

Use "execute" to execute letstrust-ssi-core with no arguments. If you don't supply any arguments of {build|build-docker|build-podman|extract|execute}, letstrust-ssi-core will be executed with the provided arguments.

### Code-level integration

For directly integrating the library as Java dependency, please refer to project _LetsTrust Examples_ which contains a project configuration for Gradle and Maven es well es several examples how the library can be used.

## :hammer: Build

For building the project the build tool Gradle as well as a Java 15 dev-env needs to be available.

### Gradle build

For building the project JDK 15 or above is required.

#### Building the application:

    letstrust.sh build

#### Manually:

    gradle clean assemble

After the Gradle build the program can be run as follows: In `build/distributions/` you have two archives, a .tar, and a .zip.  
Extract either one of them, and execute `letstrust-ssi-core-1.0-SNAPSHOT/bin/letstrust-ssi-core`.

    cd build/distributions
    tar xf letstrust-ssi-core-1.0-SNAPSHOT.tar    # or unzip for the .zip
    cd letstrust-ssi-core-1.0-SNAPSHOT/bin

    ./letstrust-ssi-core

### Docker build

#### Optionally building the Docker container afterwards:

    letstrust.sh build-docker

#### Manually (required Gradle build):

    docker build -t letstrust .

### Podman build

#### Also works with rootless podman:

    letstrust.sh build-podman

#### Manually (required Gradle build):

    podman build -t letstrust .

## :page_facing_up:  Configuration

The default-configuration is set to the following values:

````
keystore:
  type: database # allowed values: 'file', 'database' or 'custom'

essif:
  essifApiBaseUrl: "https://api.ebsi.xyz"
  authorizationApi: "/authorization/v1"
  ledgerAPI: "/ledger/v1"
  trustedIssuerRegistryApi: "/tir/v2"
  trustedAccreditationOrganizationRegistryApi: "/taor/v1"
  revocationRegistry: "/revocation/v1"
  schemaRegistry: "/revocation/v1"

server:
  host: 0.0.0.0
  port: 8080

hikariDataSource:
  dataSourceClassName: org.sqlite.SQLiteDataSource
  jdbcUrl: jdbc:sqlite:data/letstrust.db
  maximumPoolSize: 5
  autoCommit: false
  dataSource:
    journalMode: WAL
    fullColumnNames: false
````

In order to overwrite these values, simply place a yaml-based config-file named `letstrust.yaml` in the root folder with the desired values.


## :bulb: Example Commands

### For getting help, add "-h" to each command or sub-command e.g.:
    ./letstrust.sh did create -h
    or
    docker run -it -v $(pwd)/data:/app/data letstrust did create -h

### For debug infos add "-v" e.g.:

    ./letstrust.sh -v
    or
    docker run -it -v $(pwd)/data:/app/data letstrust -v did create

### Overwriting the default config:
    Simply add a file named _lestrust.yaml_ in the root folder and run ./letstrust.sh

    When using Docker, the following command will do the trick:
    docker run -it $(pwd)/data:/app/data -v $(pwd)/letstrust.yaml:/letstrust.yaml letstrust -v did create

### LetsTrust wrapper commands

    ./letstrust.sh key gen --algorithm Ed25519

    ./letstrust.sh key list

    ./letstrust.sh did create -m web

    ./letstrust.sh did resolve --did did:web:mattr.global

    ./letstrust.sh -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    ./letstrust.sh vc verify data/vc/created/vc-1614291790088-default.json

    ./letstrust.sh -v vc present data/vc/created/vc-1614291790088-default.json

    ./letstrust.sh vc verify -p data/vc/presented/vp-1614291892489.json

### LetsTrust Docker / Podman commands
    docker run -itv $(pwd)/data:/app/data letstrust key gen --algorithm Ed25519

    docker run -itv $(pwd)/data:/app/data letstrust key list

    docker run -itv $(pwd)/data:/app/data letstrust did create -m web

    docker run -itv $(pwd)/data:/app/data letstrust did resolve --did did:web:mattr.global

    docker run -itv $(pwd)/data:/app/data letstrust -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    docker run -itv $(pwd)/data:/app/data letstrust vc verify data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/app/data letstrust -v vc present data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/app/data letstrust vc verify -p data/vc/presented/vp-1614291892489.json

    docker run -itv $(pwd)/templates:/app/templates -v $(pwd)/data:/app/data letstrust vc templates list

    docker run -itv $(pwd)/data:/app/data -p 7000-7001:7000-7001 letstrust serve

    podman run -itv $(pwd)/data:/app/data -p 7000-7001:7000-7001 letstrust serve


### EBSI DID Registration (via CLI)
Create a directory for the generated data (if not present already)

    mkdir -p data/ebsi
Paste your bearer token from https://app.preprod.ebsi.eu/users-onboarding in file *data/ebsi/bearer-token.txt*

    cat > data/ebsi/bearer-token.txt 

Use the **walt.id** command line tool for creating and registering the DID EBSI.

    "<cli-tool>" can be replaced with the startup-script "./letstrust.sh" (when running the code-base)

    e.g. ./letstrust.sh  key gen -a Secp256k1

    "<cli-tool>" can be replaced with  "docker run -itv $(pwd)/data:/app/data" (when running Docker)

    In case of Docker add: "-p 7000-7001:7000-7001" when the REST APIs are required.
    In case of Docker add: "-v $(pwd)/templates:/app/templates" when VC templates should be used.

    e.g. docker run -itv $(pwd)/data:/app/data -v $(pwd)/templates:/app/templates -p 7000-7001:7000-7001 letstrust vc -h


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

# Docker PUSH / PULL
**push**

    export CR_PAT=<gh-token-with-package-write-permissions>
    echo $CR_PAT | docker login ghcr.io -u <YOUR-USER-NAME> --password-stdin
    docker tag letstrust ghcr.io/letstrustid/letstrust:test
    docker push ghcr.io/letstrustid/letstrust:test

**pull**

    export CR_PAT=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
    echo $CR_PAT | docker login ghcr.io
    docker pull ghcr.io/letstrustid/letstrust:test
    docker tag ghcr.io/letstrustid/letstrust:test letstrust
    docker run -itv $(pwd)/data:/app/data -p 7000-7001:7000-7001 letstrust serve
    
    podman pull ghcr.io/letstrustid/letstrust:test
    podman tag ghcr.io/letstrustid/letstrust:test letstrust
    podman run -itv $(pwd)/data:/app/data -itv $(pwd)/templates:/app/templates -p 7000-7001:7000-7001 letstrust serve



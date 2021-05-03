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

    docker pull ghcr.io/letstrustid/letstrust:0.1
    docker tag ghcr.io/letstrustid/letstrust:0.1 letstrust

Run via Docker:

    docker run -itv $(pwd)/data:/opt/data letstrust -h

Run via Podman:

    mkdir data  # directory where the data is stored needs do be created manually
    podman run -itv $(pwd)/data:/opt/data letstrust

### LetsTrust Wrapper

Alternatively the LetsTrust wrapper script **letstrust.sh** is a convenient way for building and using the library. This requires [building](#hammer-build) (see below) the project and a Java 15 runtime environment.

    letstrust.sh [arguments...]

### Code-level integration

For directly integrating the library as Java dependency, please refer to project _LetsTrust Examples_ which contains a project configuration for Gradle and Maven es well es several examples how the library can be used.

## :hammer: Build

For building the project the build tool Gradle as well as a Java 15 dev-env needs to be available.

### Gradle build

#### Building the application:

    letstrust.sh build

#### Manually:

    gradle clean assemble

After the Gradel build the program can be run as follows: In `build/distributions/` you have two archives, a .tar, and a .zip.  
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
    docker run -it -v $(pwd)/data:/opt/data letstrust did create -h

### For debug infos add "-v" e.g.:

    ./letstrust.sh -v
    or
    docker run -it -v $(pwd)/data:/opt/data letstrust -v did create

### Overwriting the default config:
    Simply add a file named _lestrust.yaml_ in the root folder and run ./letstrust.sh

    When using Docker, the following command will do the trick:
    docker run -it $(pwd)/data:/opt/data -v $(pwd)/letstrust.yaml:/letstrust.yaml letstrust -v did create

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
    docker run -itv $(pwd)/data:/opt/data letstrust key gen --algorithm Ed25519

    docker run -itv $(pwd)/data:/opt/data letstrust key list

    docker run -itv $(pwd)/data:/opt/data letstrust did create -m web

    docker run -itv $(pwd)/data:/opt/data letstrust did resolve --did did:web:mattr.global

    docker run -itv $(pwd)/data:/opt/data letstrust -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    docker run -itv $(pwd)/data:/opt/data letstrust vc verify data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/opt/data letstrust -v vc present data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/opt/data letstrust vc verify -p data/vc/presented/vp-1614291892489.json

    docker run -itv $(pwd)/data:/opt/data -p 7000-7001:7000-7001 letstrust serve

    podman run -itv $(pwd)/data:/opt/data -p 7000-7001:7000-7001 letstrust serve

## letstrust-ssi-core wrapper script

Usage:
    
    ./letstrust.sh {build|build-docker|build-podman|extract|execute (default)}

Use "execute" to execute letstrust-ssi-core with no arguments. If you don't supply any
arguments of {build|build-docker|build-podman|extract|execute}, letstrust-ssi-core will
be executed with the provided arguments.

#Docker PUSH / PULL
**push**

export CR_PAT=<token-write-packages>
echo $CR_PAT | docker login ghcr.io -u <username> --password-stdin
docker tag letstrust ghcr.io/letstrustid/letstrust:0.1
docker push ghcr.io/letstrustid/letstrust:0.1

**pull**

export CR_PAT=<token-read-packages>
docker pull ghcr.io/letstrustid/letstrust:0.1
docker tag ghcr.io/letstrustid/letstrust:0.1 letstrust
docker run letstrust


podman pull ghcr.io/letstrustid/letstrust:0.1
podman tag ghcr.io/letstrustid/letstrust:0.1 letstrust
podman run letstrust

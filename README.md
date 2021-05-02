# LetsTrust SSI Core

Kotlin/Java library & dockerized CLI tool for SSI core services, with primary focus on the European EBSI/ESSIF ecosystem.

The core services are in the scope of:
 - **Key Management**
 - **Decentralized Identifier (DID) operations (register, update, deactivate)**
 - **Verifiable Credential (VC) operations (issue, present, verify)**

## :hammer: Build

### Gradle build

#### Building the application:

    letstrust.sh build

#### Manually:

    gradle clean assemble

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


## :gear: Run

### Running CLI tool directly (requires Java 15):

#### Letstrust wrapper

    letstrust.sh [arguments...]

#### Manually (requires Gradle build)

In `build/distributions/` you have two archives, a .tar, and a .zip.  
Extract either one of them, and execute `letstrust-ssi-core-1.0-SNAPSHOT/bin/letstrust-ssi-core`.

e.g.:

    cd build/distributions
    tar xf letstrust-ssi-core-1.0-SNAPSHOT.tar    # or unzip for the .zip
    cd letstrust-ssi-core-1.0-SNAPSHOT/bin

    ./letstrust-ssi-core

### Run CLI tool via Podman:
    mkdir data  # directory where the data is stored needs do be created manually

    podman run -itv $(pwd)/data:/opt/data letstrust

### Run CLI tool via Docker:

    docker run -itv $(pwd)/data:/opt/data letstrust

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

### Examples Startup-Scrip 

    ./letstrust.sh key gen --algorithm Ed25519

    ./letstrust.sh key list

    ./letstrust.sh did create -m web

    ./letstrust.sh did resolve --did did:web:mattr.global

    ./letstrust.sh -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    ./letstrust.sh vc verify data/vc/created/vc-1614291790088-default.json

    ./letstrust.sh -v vc present data/vc/created/vc-1614291790088-default.json

    ./letstrust.sh vc verify -p data/vc/presented/vp-1614291892489.json

### Examples Docker / Podman
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

### letstrust-ssi-core wrapper script

Usage:
    
    ./letstrust.sh {build|build-docker|build-podman|extract|execute (default)}

Use "execute" to execute letstrust-ssi-core with no arguments. If you don't supply any
arguments of {build|build-docker|build-podman|extract|execute}, letstrust-ssi-core will
be executed with the provided arguments.

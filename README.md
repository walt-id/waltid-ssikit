# Let's Trust SSI Core

Kotlin/Java library & dockerized CLI tool for SSI core services, with primary focus on European EBSI/ESSIF ecosystem.

The core services are in the scope of:
 - **key-management**
 - **signing**
 - **encryption**
 - **DID & VC operations**

## Installation (using Docker)
    docker pull letstrust/test
    docker tag letstrust/test letstrust

## :hammer: Build

### Building the application
    gradle clean assemble

### Building the Docker container
    docker build -t letstrust .

### Pushing the Docker container
    docker tag letstrust letstrust/test
    docker push letstrust/test

## Configuration

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

### Running the application directly:

In `build/distributions/` you have two archives, a .tar, and a .zip.  
Extract either one of them, and execute `letstrust-ssi-core-1.0-SNAPSHOT/bin/letstrust-ssi-core`.

e.g.:

    cd build/distributions
    tar xf letstrust-ssi-core-1.0-SNAPSHOT.tar    # or unzip for the .zip
    cd letstrust-ssi-core-1.0-SNAPSHOT/bin

    ./letstrust-ssi-core

### Run CLI tools via Docker:
    docker run -it -v $(pwd)/data:/data letstrust

#### Via Docker including an optional config-file called `letstrust.yaml`:
    docker run -it -v $(pwd)/data:/data -v $(pwd)/letstrust.yaml:/letstrust.yaml letstrust -v did create

#### For getting help, add "-h" to each command or sub-command e.g.:
    docker run -it -v $(pwd)/data:/data letstrust did create -h

#### For debug infos add "-v" e.g.:

    docker run -it -v $(pwd)/data:/data letstrust -v did create

#### Examples
    docker run -it -v $(pwd)/data:/data letstrust key gen --algorithm Ed25519

    docker run -it -v $(pwd)/data:/data letstrust key list

    docker run -it -v $(pwd)/data:/data letstrust did create -m web

    docker run -it -v $(pwd)/data:/data letstrust did resolve --did did:web:mattr.global

    docker run -it -v $(pwd)/data:/data letstrust -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    docker run -it -v $(pwd)/data:/data letstrust vc verify data/vc/created/vc-1614291790088-default.json

    docker run -it -v $(pwd)/data:/data letstrust -v vc present data/vc/created/vc-1614291790088-default.json

    docker run -it -v $(pwd)/data:/data letstrust vc verify -p data/vc/presented/vp-1614291892489.json



# Walt.ID SSI Kit

SSI core services, with primarily focus on the European EBSI/ESSIF ecosystem.

The core services are in the scope of:
 - **Key Management** generation, import/export
 - **Decentralized Identifier (DID)** operations (register, update, deactivate)
 - **Verifiable Credential (VC)** operations (issue, present, verify)
 - **ESSIF/EBSI** related Use Cases (onboarding, VC exchange, etc.)


The **Kotlin/Java based library** can be directly integrated in Java apps. Alternatively the library or the additional **Docker container** can be run as RESTful webservice.

The **CLI tool** conveniently allows running all included functions manually. 

## :unlock: Usage

### Walt Container

The simplest way of using _Walt.ID SSI Kit_ library is by pulling the Docker Container an running it via **Docker** or **Podman**. 

Login to Container Registry:

    export CR_PAT=<token-read-packages>
    docker login

Run via Docker:

    docker run -itv $(pwd)/data:/app/data walt -h

Run via Podman:

    mkdir data  # directory where the data is stored needs do be created manually
    podman run -itv $(pwd)/data:/app/data walt

Run as RESTful service via Docker Compose:

    docker-compose build
    docker-compose up


### Walt Wrapper

Alternatively the Walt wrapper script **walt.sh** is a convenient way for building and using the library on **Linux**. This requires [building](#hammer-build) (see below) the project and a Java 16 runtime environment.

    ./walt.sh {build|build-docker|build-podman|extract|execute (default)}

Use "execute" to execute waltid-ssi-kit with no arguments. If you don't supply any arguments of {build|build-docker|build-podman|extract|execute}, waltid-ssi-kit will be executed with the provided arguments.

### Code-level integration

For directly integrating the library as Kotlin/Java dependency, please refer to the project [waltid-ssi-kit-examples](https://github.com/walt-id/waltid-ssikit-examples). The examples show how the library can be used in Kotlin as well as Java projects.

## :hammer: Build

For building the project the build tool Gradle as well as a Java 16 dev-env needs to be available. If you need the library for a lower version, please contact us.

### Gradle build

For building the project, JDK 16 or above is required.

#### Building the application:

    walt.sh build

#### Manually:

    gradle clean build

After the Gradle build the program can be run as follows: In `build/distributions/` you have two archives, a .tar, and a .zip.  
Extract either one of them, and execute `waltid-ssi-kit-1.0-SNAPSHOT/bin/waltid-ssi-kit`.

    cd build/distributions
    tar xf waltid-ssi-kit-1.0-SNAPSHOT.tar    # or unzip for the .zip
    cd waltid-ssi-kit-1.0-SNAPSHOT/bin

    ./waltid-ssi-kit

### Docker build

#### Building the Docker container:

    walt.sh build-docker

#### Manually:

    docker build -t walt .

### Podman build

#### Also works with rootless podman:

    walt.sh build-podman

#### Manually:

    podman build -t walt .

## :page_facing_up:  Configuration

Services come with their own configuration files. For the configuration of service -> implementation mappings, [ServiceMatrix](https://github.com/walt-id/service-matrix) is used.

The default mapping file is "service-matrix.properties", and looks like this:

```properties
id.walt.services.vc.VCService=id.walt.services.vc.WaltIdVCService
id.walt.services.crypto.CryptoService=id.walt.services.crypto.SunCryptoService
id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.SqlKeyStoreService
id.walt.services.key.KeyService=id.walt.services.key.WaltIdKeyService
```

e.g., to change the keystore service, simply replace the line `id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.SqlKeyStoreService` with your own implementation mapping, e.g. for the Azure HSM keystore: `id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.azurehsm.AzureHSMKeystoreService`

To add a service configuration: `id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.SqlKeyStoreService:sql.conf`
Service configuration is by default in HOCON format.
Refer to the specific service on how their configuration is laid out.


## :bulb: Example Commands

### For getting help, add "-h" to each command or sub-command e.g.:
    ./walt.sh did create -h
    or
    docker run -it -v $(pwd)/data:/app/data walt did create -h

### For debug infos add "-v" e.g.:

    ./walt.sh -v
    or
    docker run -it -v $(pwd)/data:/app/data walt -v did create

### Overwriting the default config:
    Simply add a file named _lestrust.yaml_ in the root folder and run ./walt.sh

    When using Docker, the following command will do the trick:
    docker run -it $(pwd)/data:/app/data -v $(pwd)/walt.yaml:/walt.yaml walt -v did create

### Walt wrapper commands

    ./walt.sh key gen --algorithm Ed25519

    ./walt.sh key list

    ./walt.sh did create -m web

    ./walt.sh did resolve --did did:web:mattr.global

    ./walt.sh -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    ./walt.sh vc verify data/vc/created/vc-1614291790088-default.json

    ./walt.sh -v vc present data/vc/created/vc-1614291790088-default.json

    ./walt.sh vc verify -p data/vc/presented/vp-1614291892489.json

### Walt Docker / Podman commands
    docker run -itv $(pwd)/data:/app/data walt key gen --algorithm Ed25519

    docker run -itv $(pwd)/data:/app/data walt key list

    docker run -itv $(pwd)/data:/app/data walt did create -m web

    docker run -itv $(pwd)/data:/app/data walt did resolve --did did:web:mattr.global

    docker run -itv $(pwd)/data:/app/data walt -v vc issue --issuer-did did:key:z6MkmNMF2... --subject-did did:key:zjkl2sd...

    docker run -itv $(pwd)/data:/app/data walt vc verify data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/app/data walt -v vc present data/vc/created/vc-1614291790088-default.json

    docker run -itv $(pwd)/data:/app/data walt vc verify -p data/vc/presented/vp-1614291892489.json

    docker run -itv $(pwd)/templates:/app/templates -v $(pwd)/data:/app/data walt vc templates list

    docker run -itv $(pwd)/data:/app/data -p 7000-7001:7000-7001 walt serve

    podman run -itv $(pwd)/data:/app/data -p 7000-7001:7000-7001 walt serve


### EBSI DID Registration (via CLI)
Create a directory for the generated data (if not present already)

    mkdir -p data/ebsi
Paste your bearer token from https://app.preprod.ebsi.eu/users-onboarding in file *data/ebsi/bearer-token.txt*

    cat > data/ebsi/bearer-token.txt 

Use the **walt.id** command line tool for creating and registering the DID EBSI.

    "<cli-tool>" can be replaced with the startup-script "./walt.sh" (when running the code-base)

    e.g. ./walt.sh  key gen -a Secp256k1

    "<cli-tool>" can be replaced with  "docker run -itv $(pwd)/data:/app/data" (when running Docker)

    In case of Docker add: "-p 7000-7001:7000-7001" when the REST APIs are required.
    In case of Docker add: "-v $(pwd)/templates:/app/templates" when VC templates should be used.

    e.g. docker run -itv $(pwd)/data:/app/data -v $(pwd)/templates:/app/templates -p 7000-7001:7000-7001 walt vc -h


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


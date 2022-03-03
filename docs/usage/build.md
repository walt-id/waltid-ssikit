# Build

For building the project **Gradle 7** as well as **JDK 16 (or above)** is required.

#### Building the application

First clone the Git repo and switch into the project folder:

    git clone https://github.com/walt-id/waltid-ssikit.git
    cd waltid-ssikit/

##### walt.id Wrapper

The walt.id wrapper script **ssikit.sh** is a convenient way for building and using the library on **Linux**.

    ./ssikit.sh {build|build-docker|build-podman|extract|execute (default)}

The script takes one of the the following arguments: build|build-docker|build-podman|extract|execute.

For example, for building the project, simply supply the "build" argument:

    ./ssikit.sh build

#### Manually with gradle:

    gradle clean build

After the Gradle build you can run the executable. In `build/distributions/` you have two archives, a .tar, and a .zip.  
Extract either one of them, and run `waltid-ssi-kit-1.0-SNAPSHOT/bin/waltid-ssi-kit`.

    cd build/distributions
    tar xf waltid-ssi-kit-1.0-SNAPSHOT.tar    # or unzip for the .zip
    cd ../..  # go back to the root-directory

    ./build/distributions/waltid-ssi-kit-1.0-SNAPSHOT/bin/waltid-ssi-kit

#### Building the Docker Container

    ./ssikit.sh build-docker

or with Podman

    ./ssikit.sh build-podman

or without script

    docker build -t ssikit .


#### Manually:

    podman build -t ssikit .

### Docker Compose

Run as RESTful service via Docker Compose:

    docker-compose build
    docker-compose up

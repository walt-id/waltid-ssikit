---
description: Getting started with the command line interface
---

# Quick start

The simplest way of using _Walt.ID SSI Kit_ library is by pulling the Docker Container and running it via **Docker**:

```
docker run -itv $(pwd)/data:/app/data waltid/ssikit -h
```

Use the wrapper script **ssikit.sh** on Linux (requires Gradle 7 & Java 16) for building the source and running the CLI tool:

```
git clone https://github.com/walt-id/waltid-ssikit.git
cd waltid-ssikit/
./ssikit.sh -h
```

For a quick intro using the command line interface, refer to:

{% content-ref url="cli.md" %}
[cli.md](cli.md)
{% endcontent-ref %}

## Build

For building the project **Gradle 7** as well as **JDK 16 (or above)** is required.

#### Building the application

First clone the Git repo and switch into the project folder:

```
git clone https://github.com/walt-id/waltid-ssikit.git
cd waltid-ssikit/
```

The walt.id wrapper script **ssikit.sh** is a convenient way for building and using the library on **Linux**.

```
./ssikit.sh {build|build-docker|build-podman|extract|execute (default)}
```

The script takes one of the the following arguments: build|build-docker|build-podman|extract|execute.

For example, for building the project, simply supply the "build" argument:

```
./ssikit.sh build
```

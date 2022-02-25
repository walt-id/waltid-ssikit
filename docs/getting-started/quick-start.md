---
description: Getting started with the SSIKit
---

# Quick start

## Run

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

### Prerequisites

* JDK 16 (or above)
* Git

### Building the application

The easiest way to build the SSIKit on Linux, is by using the wrapper script **`ssikit.sh`**

First clone the Git repo and switch into the project folder:

```
git clone https://github.com/walt-id/waltid-ssikit.git
cd waltid-ssikit/
```

Then run the build command using the wrapper script:

```
./ssikit.sh build
```

For more detailed information about the build process and build options, refer to:

{% content-ref url="../usage/build.md" %}
[build.md](../usage/build.md)
{% endcontent-ref %}

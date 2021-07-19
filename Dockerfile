FROM docker.io/openjdk:15-slim-buster AS openjdk-gradle

ENV GRADLE_HOME /opt/gradle

RUN set -o errexit -o nounset \
    && echo "Adding gradle user and group" \
    && groupadd --system --gid 1000 gradle \
    && useradd --system --gid gradle --uid 1000 --shell /bin/bash --create-home gradle \
    && mkdir /home/gradle/.gradle \
    && chown --recursive gradle:gradle /home/gradle \
    \
    && echo "Symlinking root Gradle cache to gradle Gradle cache" \
    && ln -s /home/gradle/.gradle /root/.gradle

VOLUME /home/gradle/.gradle

WORKDIR /opt

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        fontconfig \
        unzip \
        wget \
        \
        bzr \
        git \
        mercurial \
        openssh-client \
        subversion \
    && rm -rf /var/lib/apt/lists/*

ENV GRADLE_VERSION 6.5.1
ARG GRADLE_DOWNLOAD_SHA256=50a7d30529fa939721fe9268a0205142f3f2302bcac5fb45b27a3902e58db54a
RUN set -o errexit -o nounset \
    && echo "Downloading Gradle" \
    && wget --no-verbose --output-document=gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    \
    && echo "Checking download hash" \
    && echo "${GRADLE_DOWNLOAD_SHA256} *gradle.zip" | sha256sum --check - \
    \
    && echo "Installing Gradle" \
    && unzip gradle.zip \
    && rm gradle.zip \
    && mv "gradle-${GRADLE_VERSION}" "${GRADLE_HOME}/" \
    && ln --symbolic "${GRADLE_HOME}/bin/gradle" /usr/bin/gradle \
    \
    && echo "Testing Gradle installation" \
    && gradle --version

RUN /bin/bash -c "source $HOME/.bashrc"

FROM openjdk-gradle AS letstrust-build
COPY ./ /opt
RUN gradle clean assemble
RUN tar xf /opt/build/distributions/letstrust-ssi-core-1.0-SNAPSHOT.tar -C /opt

RUN mkdir /app
RUN mv /opt/letstrust-ssi-core-1.0-SNAPSHOT/* /app
RUN rm -r /opt/*

WORKDIR /app

ENTRYPOINT ["/app/bin/letstrust-ssi-core"]

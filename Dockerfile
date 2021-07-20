FROM docker.io/openjdk:16-slim-buster AS openjdk-gradle

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

RUN apt-get update

FROM openjdk-gradle AS letstrust-build
COPY ./ /opt
RUN ./gradlew clean assemble
RUN tar xf /opt/build/distributions/letstrust-ssi-core-1.0-SNAPSHOT.tar -C /opt

RUN mkdir /app
RUN mv /opt/letstrust-ssi-core-1.0-SNAPSHOT/* /app
RUN rm -r /opt/*

WORKDIR /app

ENTRYPOINT ["/app/bin/letstrust-ssi-core"]

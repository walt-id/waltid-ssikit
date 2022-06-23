FROM docker.io/openjdk:17-slim-buster AS openjdk-gradle

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

RUN apt-get update && apt-get upgrade --yes

FROM openjdk-gradle AS walt-build
COPY ./ /opt
RUN ./gradlew clean build
RUN tar xf /opt/build/distributions/waltid-ssi-kit-*.tar -C /opt

FROM openjdk:17-slim-buster

ADD https://openpolicyagent.org/downloads/v0.41.0/opa_linux_amd64_static /usr/local/bin/opa
RUN chmod 755 /usr/local/bin/opa

RUN mkdir /app
COPY --from=walt-build /opt/waltid-ssi-kit-* /app/
COPY --from=walt-build /opt/service-matrix.properties /app/
COPY --from=walt-build /opt/signatory.conf /app/
COPY --from=walt-build /opt/fsStore.conf /app/

WORKDIR /app

ENTRYPOINT ["/app/bin/waltid-ssi-kit"]

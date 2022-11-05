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
RUN apt-get install -y dos2unix

FROM openjdk-gradle AS walt-build
COPY ./ /opt

# When running docker build on Windows, make sure these have
# the right line endings

RUN dos2unix ./gradlew src/test/resources/key/*.pem

RUN ./gradlew clean build
RUN tar xf /opt/build/distributions/waltid-ssi-kit-*.tar -C /opt

FROM waltid/waltid_iota_identity_wrapper:latest as iota_wrapper
FROM openjdk:17-slim-buster

ADD https://openpolicyagent.org/downloads/v0.41.0/opa_linux_amd64_static /usr/local/bin/opa
RUN chmod 755 /usr/local/bin/opa

COPY --from=iota_wrapper /usr/local/lib/libwaltid_iota_identity_wrapper.so /usr/local/lib/libwaltid_iota_identity_wrapper.so
RUN ldconfig
RUN mkdir /app
COPY --from=walt-build /opt/waltid-ssi-kit-* /app/
COPY --from=walt-build /opt/service-matrix.properties /app/
COPY --from=walt-build /opt/signatory.conf /app/
COPY --from=walt-build /opt/fsStore.conf /app/

WORKDIR /app

ENTRYPOINT ["/app/bin/waltid-ssi-kit"]

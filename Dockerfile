FROM openjdk:15-alpine
COPY build/distributions/letstrust-ssi-core-1.0-SNAPSHOT.tar /opt/letstrust.tar
RUN tar xf /opt/letstrust.tar -C /opt/
RUN rm /opt/letstrust.tar

WORKDIR /opt/letstrust-ssi-core-1.0-SNAPSHOT
ENTRYPOINT bin/letstrust-ssi-core

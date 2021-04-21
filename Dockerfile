FROM openjdk:15-slim-buster
COPY build/distributions/letstrust-ssi-core-1.0-SNAPSHOT.tar /opt/letstrust.tar
RUN tar xf /opt/letstrust.tar -C /opt/
RUN mv /opt/letstrust-ssi-core-1.0-SNAPSHOT/* /opt
RUN rm -r /opt/letstrust.tar /opt/letstrust-ssi-core-1.0-SNAPSHOT

WORKDIR /opt
ENTRYPOINT ["bin/letstrust-ssi-core"]

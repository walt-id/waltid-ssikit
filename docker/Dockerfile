FROM openjdk:14-slim-buster
WORKDIR /
ADD  ./target/letstrust-ssi-core-1.0-SNAPSHOT-jar-with-dependencies.jar letstrust.jar
ENTRYPOINT ["java","-jar","/letstrust.jar"]

FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.9.2-SNAPSHOT"

LABEL name="snowizard" version=$VERSION

ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1

RUN mkdir -p /opt/snowizard
WORKDIR /opt/snowizard
COPY ./snowizard.jar /opt/snowizard
COPY ./snowizard-application/snowizard.yml /opt/snowizard/snowizard.yml
VOLUME ["/opt/snowizard"]

EXPOSE 8080 8180
ENTRYPOINT ["java", "-d64", "-server", "-jar", "snowizard.jar"]
CMD ["server", "snowizard.yml"]

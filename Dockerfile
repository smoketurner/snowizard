FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.9.2-SNAPSHOT"

LABEL name="snowizard" version=$VERSION

ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1

ENV PORT 8080

WORKDIR /app

COPY pom.xml .

RUN apk add --no-cache openjdk8="$JAVA_ALPINE_VERSION" && \
    mvnw install

COPY . .

RUN mvnw package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dmaven.source.skip=true && \
    rm snowizard-application/target/original-*.jar && \
    mv snowizard-application/target/*.jar app.jar && \
    rm -rf /root/.m2 && \
    rm -rf snowizard-application/target && \
    rm -rf snowizard-client/target && \
    rm -rf snowizard-api/target && \
    rm -rf snowizard-core/target && \
    apk del openjdk8

CMD java $JAVA_OPTS -Ddw.server.applicationConnectors[0].port=$PORT -jar app.jar server config.yml

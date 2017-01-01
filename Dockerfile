FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="2.0.0-SNAPSHOT"

LABEL name="snowizard" version=$VERSION

ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1
ENV PORT 8080

RUN apk add --no-cache openjdk8="$JAVA_ALPINE_VERSION"

WORKDIR /app

RUN mkdir -p snowizard-api snowizard-application snowizard-core snowizard-client

COPY pom.xml mvnw ./
COPY .mvn ./.mvn/
COPY snowizard-api/pom.xml ./snowizard-api/
COPY snowizard-application/pom.xml ./snowizard-application/
COPY snowizard-core/pom.xml ./snowizard-core/
COPY snowizard-client/pom.xml ./snowizard-client/

RUN ./mvnw install

COPY . .

RUN ./mvnw package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm snowizard-application/target/original-*.jar && \
    mv snowizard-application/target/*.jar app.jar && \
    rm -rf /root/.m2 && \
    rm -rf snowizard-application/target && \
    rm -rf snowizard-client/target && \
    rm -rf snowizard-api/target && \
    rm -rf snowizard-core/target && \
    apk del openjdk8

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]

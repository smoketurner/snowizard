FROM openjdk:8-jdk-alpine AS BUILD_IMAGE

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

RUN ./mvnw clean package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm snowizard-application/target/original-*.jar && \
    mv snowizard-application/target/*.jar app.jar

FROM openjdk:8-jre-alpine

ARG VERSION="2.0.0-SNAPSHOT"

LABEL name="snowizard" version=$VERSION

ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1
ENV PORT 8080

RUN apk add --no-cache curl

WORKDIR /app
COPY --from=BUILD_IMAGE /app/app.jar .
COPY --from=BUILD_IMAGE /app/config.yml .

HEALTHCHECK --interval=10s --timeout=5s CMD curl --fail http://127.0.0.1:8080/admin/healthcheck || exit 1

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]

#
# Copyright © 2013, General Electric Corporation
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of the <organization> nor the
#       names of its contributors may be used to endorse or promote products
#       derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

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

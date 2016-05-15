#!/bin/sh

VERSION="1.9.2-SNAPSHOT"

docker rm -f build-cont
docker build -t build-img -f Dockerfile.build .
docker create --name build-cont build-img
docker cp "build-cont:/src/snowizard-application/target/snowizard-application-${VERSION}.jar" ./snowizard.jar
docker build --build-arg VERSION=${VERSION} -t "smoketurner/snowizard:${VERSION}" .

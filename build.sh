#!/bin/sh

VERSION="1.9.2-SNAPSHOT"

docker build --build-arg VERSION=${VERSION} -t "smoketurner/snowizard:${VERSION}" .
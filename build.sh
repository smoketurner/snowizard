#!/bin/sh

VERSION="2.0.0-SNAPSHOT"

docker build --build-arg VERSION=${VERSION} -t "smoketurner/snowizard:${VERSION}" .
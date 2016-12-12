#!/bin/bash
set -v
if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ]; then
    exit
else
    ./gradlew -Pci --console=plain :app:testDebug -PtestSuite=SlowSuite -PbuildDir=slow
fi

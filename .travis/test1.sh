#!/bin/bash
set -v
if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ]; then
    ./gradlew -Pci --console=plain :app:testDebug
else
    ./gradlew -Pci --console=plain :app:testDebug -PtestSuite=FastSuite -PbuildDir=fast
fi

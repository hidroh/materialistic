#!/bin/bash
set -v
if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ] && [ "${TASK}" = "test1" ]; then
    ./gradlew -Pci --console=plain jacocoTestReport coveralls
fi

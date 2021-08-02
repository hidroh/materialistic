#!/bin/bash
set -v
if [ "${TRAVIS_BRANCH}" = "master" ] && [ "${TASK}" = "test" ]; then
    ./gradlew -Pci --console=plain jacocoTestReport coveralls
fi

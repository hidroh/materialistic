#!/bin/bash
set -v
if [ "${TRAVIS_BRANCH}" = "master" ] && [ "${TASK}" = "test" ]; then
    ./gradlew -Pci --console=plain jacocoTestReport coveralls
    curl -F 'json_file=@build/coveralls/report.json' 'https://coveralls.io/api/v1/jobs'
fi

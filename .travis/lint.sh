#!/bin/bash
set -v
./gradlew -Pci --console=plain :app:lintDebug -PbuildDir=lint

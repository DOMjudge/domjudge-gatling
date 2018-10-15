#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
GATLING=$DIR/gatling-charts-highcharts-bundle-3.0.0-RC4/bin/gatling.sh

URL=${1:-http://localhost/domjudge}
echo "Running gatling against: $URL"

export JAVA_OPTS="-Dbaseurl=$URL"
$GATLING --results-folder $PWD/reports --simulations-folder $PWD/simulations --resources-folder $PWD/bodies --simulation domjudge.AdminSetup
$GATLING --results-folder $PWD/reports --simulations-folder $PWD/simulations --resources-folder $PWD/bodies --simulation domjudge.ContestSimulation

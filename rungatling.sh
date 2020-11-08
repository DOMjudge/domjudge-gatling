#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Download gatling bundle from here, extract it
# https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/3.4.1/gatling-charts-highcharts-bundle-3.4.1-bundle.zip
GATLING=$DIR/gatling-charts-highcharts-bundle-3.4.1/bin/gatling.sh

URL=${1:-http://localhost/domjudge}
echo "Running gatling against: $URL"

export JAVA_OPTS="-Dbaseurl=$URL"
$GATLING --results-folder $PWD/reports --simulations-folder $PWD/simulations --resources-folder $PWD/bodies --simulation domjudge.ContestSimulation

#!/bin/bash
GATLING=~/temp/gatling-charts-highcharts-bundle-2.1.7/bin/gatling.sh


export JAVA_OPTS="-Dbaseurl=http://example.com/domjudge/"
$GATLING --results-folder $PWD/reports --simulations-folder $PWD/simulations --bodies-folder $PWD/bodies --mute  --simulation domjudge.AdminSetup
$GATLING --results-folder $PWD/reports --simulations-folder $PWD/simulations --bodies-folder $PWD/bodies --mute  --simulation domjudge.ContestSimulation -rd "'$1'"

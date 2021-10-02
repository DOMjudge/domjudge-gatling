# Benchmarking DOMjudge with gatling
This repo provides some scripts/tests for load testing your particular webserver performance of DOMjudge by generating a bunch of artificial(but semi-reasonable) requests to your server. It impersonates teams, as well as public spectators. Don't run this against a server without permission.

### Prerequisites
Gatling requires scala 2.12, and a working java.
```bash
# Make sure you have java + scala 2.12 installed. If not, install them using something like asdf:
asdf plugin add java
asdf plugin add scala
asdf install java openjdk-15.0.1
# gatling 3.4 only supports 2.12, not 2.11 or 2.13
asdf install scala 2.12.12
```

To benchmark your DOMjudge server:
```bash
# Clone this repository
git clone https://github.com/DOMjudge/domjudge-gatling
cd domjudge-gatling
# Download gatling to this directory
wget https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/3.4.1/gatling-charts-highcharts-bundle-3.4.1-bundle.zip
unzip gatling-charts-highcharts-bundle-3.4.1-bundle.zip

# In your domjudge installation, set up the database properly with the things gatling expects
bin/dj_setup_database uninstall
bin/dj_setup_database install-loadtest

# Run gatling against your server
./rungatling.sh http://localhost/domjudge  # no trailing slash
```

`ContestSimulation.scala` is the main driver for gatling. It registers a team, logs in, and then performs various actions across the site to simulate what a team might do. This includes making submissions, viewing the scoreboard, requesting clarifications, etc.

To tune how much load is generated, edit `simulations/domjudge/ContestSimulation.scala` and refer to the comments at the bottom. By default, it'll create a single "user" to run through the team workflow, and a single "spectator" and run through some steps as them.

You should have your judgehosts running during this time as well, as they can contribute a significant amount of load to the system.

# License

DOMjudge, including its documentation, is free software; you can
redistribute it and/or modify it under the terms of the GNU General
Public License as published by the Free Software Foundation; either
version 2, or (at your option) any later version. See the file
COPYING.

# Benchmarking DOMjudge with gatling
This repo provides some scripts/tests for load testing your particular webserver performance of DOMjudge by generating a bunch of artificial(but semi-reasonable) requests to your server. It impersonates teams, as well as public spectators. Don't run this against a server without permission.

To benchmark your DOMjudge server:
```bash
# Clone this repository
git clone https://github.com/ubergeek42/domjudge-gatling
cd domjudge-gatling
# Download gatling to this directory
wget https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/2.3.0/gatling-charts-highcharts-bundle-2.3.0-bundle.zip
unzip gatling-charts-highcharts-bundle-2.3.0-bundle.zip
# Run gatling against your server
./rungatling.sh http://localhost/domjudge  # no trailing slash
```

The tool is broken into two parts. The first part just sets everything up for us. It assumes a login of `admin:admin`, and enables a bunch of languages, enables self-registration, and sets up a contest.

The second part, `ContestSimulation.scala`, registers a team, logs in, and then performs various actions across the site to simulate what a team might do. This includes making submissions, viewing the scoreboard, requesting clarifications, etc.

To tune how much load is generated, edit `simulations/domjudge/ContestSimulation.scala` and refer to the comments at the bottom. By default, it'll create a single user and run through some steps as them.

You should probably start with a clean database before running this script, and wipe the database after it finishes.

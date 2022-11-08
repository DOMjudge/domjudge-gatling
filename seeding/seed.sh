USERNAME=admin
PASSWORD=real_secret_password
DJ_URL=http://localhost/domjudges

# Set data source to "config data external" so that importing things works properly
# http -a "$USERNAME":"$PASSWORD" -f PUT $DJ_URL/api/v4/config data_source=1

# Import a gatling organization(affiliation) and group(category)
http -a "$USERNAME":"$PASSWORD" -f POST $DJ_URL/api/v4/users/organizations json@seeding/organizations.json
http -a "$USERNAME":"$PASSWORD" -f POST $DJ_URL/api/v4/users/groups json@seeding/groups.json

# Load some accounts/teams
http -a "$USERNAME":"$PASSWORD" -f POST $DJ_URL/api/v4/users/accounts json@seeding/accounts.json
http -a "$USERNAME":"$PASSWORD" -f POST $DJ_URL/api/v4/users/teams json@seeding/teams.json


# The accounts/teams just need to be part of some contest, with a hello world problem. The specific name/etc is not important.
# Create the gatling contest, starting now and running for 9999 hours (It will be public, since there's no exposed api to make it private :sadface:)
cat > seeding/contest.json <<EOF
{
  "name": "gatling",
  "short-name": "gatling",
  "start-time": "$(date -Iseconds)",
  "duration": "9999:00:00.000",
  "penalty-time": 20
}
EOF
http -a "$USERNAME":"$PASSWORD" -f POST $DJ_URL/api/v4/contests json@seeding/contest.json

# Add a problem to the contest
http -a "$USERNAME":"$PASSWORD" -f POST $DJ_URL/api/v4/contests/gatling/problems zip@bodies/hello-testcase.zip

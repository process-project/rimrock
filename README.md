# Rimrock - Robust Remote Process Controller

TODO

## Requirements

+ Ubuntu
+ Java 8+
+ PostgreSQL

More details are in the [requirements doc](doc/install/requirements.md).

## Installation

Please see the [installation manual](doc/install/installation.md).

## Development

Please see the [development manual](doc/development/README.md).

## Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new pull request

##Releasing

1. Merge all branches which are to be released into the develop branch.
2. Update CHANGELOG.
3. Set the release version number with `mvn versions:set -DnewVersion=x.x.x`.
4. Commit the version number change with `mvn versions:commit`.
5. Merge the develop branch into the master branch.
6. Create a tag named `x.x.x`.
7. Switch back to the develop branch and update version to next SNAPSHOT.

##Deploying

1. The final jar file is an init.d compliant executable script and can start the application by a user who is the file owner.
2. Copy the final jar file to a known location and make it executable with `chmod +x {jar-file-location}`.
3. Create a symlink with `sudo ln -s {jar-file-location} /etc/init.d/rimrock`.
4. Start the application with `sudo /etc/init.d/start` (logs are by default written to `/var/log/rimrock.log`).
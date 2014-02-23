horta hell
==========

horta hell is XMPP bot. It is based on the Akka framework.

Using
-----

### Configuration

Copy `horta.properties.example` file to `horta.properties` and tune it. All options should be self-explanatory.

Horta designed to use the embedded H2 database. You may tune the `storage` parameter group in the configuration file.

### Running

    $ sbt run

### Building for deployment

    $ sbt one-jar

### Command system

Commands may be entered in MUC (multi-user chat) or private chat with bot (private commands do not work for now).

Command syntax:

    $command argument1 argument2

or

    command/argument1/argument2/

Characters inside every argument and the command name itself may be escaped with the `\`.

Known command list:

* `$access` - diagnostic command. Prints current user access level.

* `$bash` - shows a random quote from http://bash.im.

* `$fortune` - shows a fortune from @rexim database.

* `$pet` - Tamagochi-like plugin, contains internal subcommand system. Enter `$pet help` for details. Distinct pets are
created for every room.

* `$s what_to_replace replace_with` - designed to be similar to Perl `s///` command. Replaces first argument with the
second argument, taking the last sender phrase as the base.

* `$say` - query the Markov network (generated for the sender) to generate random phrase.

* `$test` - just a test command.

* `$version` - tells the code version (unfortunately, this won't work in `sbt run` mode - only when run from jar).

### Log parser

`MarkovPlugin` (the one providing a `$say` command) uses the log parser. Parser loads the conference log from
`log_directory/conference_jid`, where `log_directory` is taken from the configuration file. File read in the
`log_encoding`. Every user phrase should be in the following format:

    [timestamp] User name: phrase containing \r as line separators if needed

Subsequent phrases should be separated with `\r\n`.

Contributing
------------

horta development is open process and we're glad to accept any suggestions and pull requests. When contributing please
keep in mind our branching model. We're trying to follow the `git-flow` one. E.g. we have two main branches: `master`
and `develop`.

Code in the `master` branch should be as stable as possible. It is constantly deployed on our server and mostly tested.

Code in the `develop` branch is our "trunk" - new features should be merged here and tested by the developers (and by
the CI system if present).

When forking, please create new branches from the `develop` branch.

Report any bugs to [the issues list](https://github.com/codingteam/horta-hell/issues). You may ask for support in our
XMPP conference: `codingteam@conference.jabber.ru`.
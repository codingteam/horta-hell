horta hell [![BuildStatus](https://travis-ci.org/codingteam/horta-hell.png?branch=develop)](https://travis-ci.org/codingteam/horta-hell)
==========
[![codingteam/horta-hell](http://issuestats.com/github/codingteam/horta-hell/badge/pr?style=flat-square)](http://www.issuestats.com/github/codingteam/horta-hell) [![codingteam/horta-hell](http://issuestats.com/github/codingteam/horta-hell/badge/issue?style=flat-square)](http://www.issuestats.com/github/codingteam/horta-hell)

horta hell is XMPP bot. It is based on the Akka framework.

Using
-----

### Configuration

Copy `horta.properties.example` file to `horta.properties` and tune it. All options should be self-explanatory.

Horta designed to use the embedded H2 database. You may tune the `storage` parameter group in the configuration file.
Note that `AUTO_SERVER` parameter should be set to `TRUE` for coordinated database access of multiple applications.

By default horta will use the configuration file `horta.properties` from the current directory but it will read the path
to the configuration file from the program arguments.

### Deployment

Deployment process management is the purpose of [horta-foundation](https://github.com/codingteam/horta-foundation)
project. It is the recommended way of horta deployment.

For development purposes, you may use the `sbt run` command or any IDE-compatible run activities.

### Building

The production-ready build can be created with the following command:

    sbt assembly

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

* `$diag participants` - participant diagnostic command. 

* `$fortune` - shows a fortune from @rexim database.

* `$link` - shows a HTML document title from provided URL.

* `$pet` - Tamagochi-like plugin, contains internal subcommand system. Enter `$pet help` for details. Distinct pets are
created for every room.

* `$s what_to_replace replace_with` - designed to be similar to Perl `s///` command. Replaces first argument with the
second argument, taking the last sender phrase as the base.

* `$say` - query the Markov network (generated for the sender) to generate random phrase.

* `$search phrase` - will search conference log for the selected phrase.

* `$send receiver message` - send the mail to the user. Please note that bot determines the receiver by nickname only.
If the nickname is not registered on server, someone else may take the nick and receive your mail. Be careful!

* `$wtf <word> [definition]` - stores a definition of a word in the
  bot's database. If used without the definition argument, shows the
  stored definition. To delete a definition use `$wtf-delete <word>`.

* `$version` - tells the code version (unfortunately, this won't work in `sbt run` mode - only when run from jar).

* `$dice <number of faces> <number of throws>` - throws the dice and returns all range of random numbers and its sum, separated with vertical bar; result example: 1 2 3 | 6. In case if no arguments are passed, returns the throwing a dice with 100 faces.

* `$karma` - karma plugin for expression of public approval. Type `$karma` for details.

Contributing
------------

horta development is open process and we're glad to accept any suggestions and pull requests. When contributing please
keep in mind our branching model. We're trying to follow the `git-flow` one. E.g. we have two main branches: `master`
and `develop`.

Code in the `master` branch should be as stable as possible. It is constantly deployed on our server and mostly tested.

Code in the `develop` branch is our "trunk" - new features should be merged here and tested by the developers (and by
the CI system if present).

When forking, please create new branches from the `develop` branch.

If you want to write new plugin for Horta, [read the following documentation](docs/How-to-write-a-plugin.md).

Report any bugs to [the issues list](https://github.com/codingteam/horta-hell/issues). You may ask for support in our
XMPP conference: `codingteam@conference.jabber.ru`.

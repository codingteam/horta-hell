horta hell
==========

horta hell is XMPP bot. It is based on the Akka framework.

## License

Copyright (C) 2013-2014 horta developers

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Running

    $ sbt run

## Building for deployment

    $ sbt one-jar

## Configuration

Tune example `horta.properties` file. All options should be self-explanatory.

## Command system

Commands may be entered in MUC (multi-user chat) or private chat with bot (private commands do not work for now).

Command syntax:

    $command argument1 argument2

or

    command/argument1/argument2/

Characters inside every argument and the command name itself may be escaped with the `\`.

Known command list:

* `$say` - query the Markov network (generated for the sender) to generate random phrase.

* `$s what_to_replace replace_with` - designed to be similar to Perl `s///` command. Replaces first argument with the second argument, taking the last
sender phrase as the base.

* `$pet` - Tamagochi-like plugin, contains complex subcommand system. Enter `$pet help` for details.

* `$version` tells the code version (unfortunately, this won't work in `sbt run` mode - only when run from jar).

* `$bash` shows a random quote from http://bash.im

## Contributing

horta development is open process and we're glad to accept any suggestions and pull requests. When contributing please
keep in mind our branching model. We're trying to follow the `git-flow` one. E.g. we have two main branches: `master`
and `develop`.

Code in the `master` branch should be as stable as possible. It is constantly deployed on our server and mostly tested.

Code in the `develop` branch is our "trunk" - new features should be merged here and tested by the developers (and by
the CI system if present).

When forking, please create new branches from the `develop` branch.

Report any bugs to [the issues list](https://github.com/codingteam/horta-hell/issues). You may ask for support in our
XMPP conference: `codingteam@conference.jabber.ru`.
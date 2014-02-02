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

## Configuration

Tune example `horta.properties` file. All options are self-explanatory.

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

* `$♥` - did you know that robots can love too?

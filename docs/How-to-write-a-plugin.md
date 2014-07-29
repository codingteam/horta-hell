How to write a plugin
=====================

This document contains information about writing a Horta plugin.

General
-------

Every plugin should be derived from the `akka.actor.Actor` trait because all Horta plugins integrates into Akka
supervision tree and should be generally treaten as actors.

To start your plugin with the Horta you should add the corresponding `akka.actor.Props` to the
`ru.org.codingteam.horta.core.Core.plugins` collection. See
[Akka documentation](http://doc.akka.io/docs/akka/2.3.4/scala/actors.html#Props) if you want to know more about
`akka.actor.Props`.

Base types
----------

TODO: Write about a `BasePlugin`.

## `CommandProcessor`

TODO.

## `MessageProcessor`

TODO.

## `ParticipantProcessor`

TODO.

## `RoomProcessor`

TODO.

Database
--------

TODO.
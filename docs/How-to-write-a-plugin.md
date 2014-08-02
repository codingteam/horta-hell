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

It is a good idea to derive your plugin from the `ru.org.codingteam.horta.plugins.BasePlugin` type. This type provides
some common plugin functionality. Inheritots should override `name` property. You also should override `dao` property if
your plugin wants to store some data in the database, see the `Database` section below.

There are some base traits that helps you to implement shared plugin functionality such as command or message
processing. These traits are described in the following sections.

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
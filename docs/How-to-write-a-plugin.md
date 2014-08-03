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

### `CommandProcessor`

Inherit from the `ru.org.codingteam.horta.plugins.CommandProcessor` trait if your plugin will process user commands.
Command is a special message sent as a private message or as a multi-user chat message. Currently Horta has two syntax
variants for commands: `$syntax` and `syntax/`. Read more about syntax variants in the user documentation.
 
To receive commands, your plugin should register them at core. Command registration is a declarative process. All you
need is to prepare immutable `ru.org.codingteam.horta.plugins.CommandDefinition` objects and put them into a `commands`
property of your actor. `CommandProcessor` will take care of all undercover machinery.

`CommandDefinition` allows you to set minimal access level for your command and to define its name (users will be able
to invoke the command by name). `CommandDefinition` should also contain a so-called *token*. Token is an object for
determining which command was invoked. Horta core will send you back this token and command arguments.

Tokens meant to be Scala case objects. *Be careful!* Do not place case objects inside any classes. Scala will link the
object and instance of the class, so every plugin instance will have its own token singleton. This behavior is known to
cause all sorts of problems. You were warned.

### `MessageProcessor`

If your plugin is supposed to receive all user messages, derive it from the
`ru.org.codingteam.horta.plugins.MessageProcessor` trait. Override the `processMessage` method and do any necessary
processing there.

### `ParticipantProcessor`

There is the `ru.org.codingteam.horta.plugins.ParticipantProcessor` trait for plugins which want to be notified of
multi-user chat participant activity (users entering and leaving the chat). Derive from this trait and override the
`processParticipantJoin` and `processParticipantLeave` methods if you want to process these messages.

### `RoomProcessor`

The last - but not least - plugin trait is `ru.org.codingteam.horta.plugins.RoomProcessor`. It will notify the plugin
when Horta enters and leaves various multi-user chats (and when some room-global events occurs). Derive from the trait
and override `processRoomJoin`, `processRoomLeave` and `processRoomTopicChange` methods to receive the notifications.   

Database
--------

If you want to persist some data in your plugin, you should use H2 database embedded in Horta.

### Database structure

To define a database structure you should create a set of SQL scripts defining the data structure. All these SQL
scripts should be stored in the `src/main/resources/db/<plugin_name>` directory (where `<plugin_name>` is the string
that is defined in the `name` property of your plugin actor). We use [FlyWay](http://flywaydb.org/) for automated data
migrations. Be sure to check [the basic documentation](http://flywaydb.org/documentation/migration/sql.html).

You should put scripts named `V<number>__Script-definition.sql` in your database directory. There scripts will be run
only once automatically when your plugin accesses the database first time.

### Data access

To access the data you should create a *data access object*. It should be derived from the
`ru.org.codingteam.horta.database` trait. This trait provides a light protocol that your plugin should follow. Then
publish your DAO to the system through overridden `BasePlugin` `dao` property.

After that, use asynchronous storage interface. Horta creates special `store` actor (you may access it with protected 
`store` property of a `BasePlugin` class). It will receive `ru.org.codingteam.horta.database.StoreObject`,
`ru.org.codingteam.horta.database.ReadObject` and `ru.org.codingteam.horta.database.DeleteObject` messages and map them
to calls of your `DAO` methods.

You should never try to create JDBC connections, data sources and such. Horta will take care of it and provide
minimalistic interface through the DAO.
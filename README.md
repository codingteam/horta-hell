vagrant-horta-hell
==================

This is a [Vagrant](http://www.vagrantup.com/) image for the
[horta-hell](https://github.com/codingteam/horta-hell)
deployment. Vagrant is a virtual enviromnent manager, horta-hell is an
XMPP bot.

Prerequisites
-------------

Install the Vagrant package for your operation system. Also you'll
need [VirtualBox](https://www.virtualbox.org/). VirtualBox here is
just the default option; there are almost nothing in the
vagrant-horta-hell that has strict dependency on VirtualBox.

Running the virtual machine
---------------------------

It is as simple as

    $ vagrant up

Configuring the horta
---------------------

You should create and place the `horta.properties` file inside the
image directory. Please take a look at the [horta
documentation](https://github.com/codingteam/horta-hell) to know more
about the configuration files. There is an example bundled with the
horta.

Please note that there should only be UNIX line endings (plain `\n`)
in the `horta.properties` file!

Currently horta uses the access to host directory for parsing the chat
logs. This directory should be defined in the host's `HORTA_LOGS`
environment variable before starting the vagrant image.

Updating the horta
------------------

This package designed to run the last stable horta version (usually
the one from the `master` branch of horta repository).

There is no default horta version embedded with the package. All
update actions should be taken manually.

When first started the virtual machine (or at any point when you're
ready to update the horta and restart it) connect to it by ssh:

    $ vagrant ssh

After that invoke the update script:

    $ sudo /vagrant/update-horta.sh

This script will take care of cloning or updating the horta source
code, (re-)compiling, (re-)installing and (re-)starting the horta.
vagrant-horta-hell
==================

This is a [Vagrant](http://www.vagrantup.com/) image for
[horta-hell](https://github.com/codingteam/horta-hell) and
[horta-web](https://github.com/codingteam/horta-hell) applications
deployment. Vagrant is a virtual enviromnent manager, horta-hell is an
XMPP bot, horta-web is a web interface for it.

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

Configuring the horta-web
-------------------------

You should place the `horta-web-app.conf` configuration file inside the
image directory. Copy the initial `conf/application.conf` file from
the [horta-web](https://github.com/codingteam/horta-hell) package and
change the paths you want.

By default horta-web will use host's 80 port for web interface
access. If you want to change this port, feel free to modify
`Vagrantfile`.

Updating the packages
---------------------

This package designed to run the last stable horta-hell and horta-web
versions (usually the ones from the `master` branches of the
corresponding repositories).

There are no default packages versions embedded with the image. All
update actions should be taken manually.

When first started the virtual machine (or at any point when you're
ready to update the horta and restart it) connect to it by ssh:

    $ vagrant ssh

After that invoke the update script:

    $ sudo /vagrant/update-horta.sh

This script will take care of cloning or updating the source code of
all embedded packages, (re-)compiling, (re-)installing and
(re-)starting the services.
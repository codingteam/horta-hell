horta-foundation
================
horta-foundation is a project for providing the default [horta-hell](https://github.com/codingteam/horta-hell)
environment ready for production.

Currently it is a [Vagrant](http://www.vagrantup.com/) image for deployment horta-hell itself and
[horta-web](https://github.com/codingteam/horta-hell).  Vagrant is a virtual environment manager, horta-hell is an XMPP
bot, horta-web is a web interface for some its functions.

Prerequisites
-------------
Install the Vagrant package for your operation system. By default you will also need
[VirtualBox](https://www.virtualbox.org/). VirtualBox is just the default option; there is nothing in the
horta-foundation that has strict dependency on VirtualBox.

Starting the virtual machine
----------------------------
It is as simple as

    $ vagrant up

Configuring the horta
---------------------
You should create and place the `horta.properties` file inside the image directory. Please take a look at the
[horta documentation](https://github.com/codingteam/horta-hell) to know more about the configuration files. There is an
example bundled with the horta-hell.

Please note that there should only be UNIX line endings (plain `\n`) in the `horta.properties` file!

Currently horta uses the access to host directory for parsing the chat logs.  This directory should be defined in the
host's `HORTA_LOGS` environment variable before starting the vagrant image. By default current directory will be mapped
if `HORTA_LOGS` is undefined.

There is an example of a simple start script `vagrant-up.ps1.example` for setting up the vagrant with an environment.

Configuring the horta-web
-------------------------
You should place the `horta-web-app.conf` configuration file inside the image directory. Copy the initial
`conf/application.conf` file from the [horta-web](https://github.com/codingteam/horta-hell) package and change the paths
you want.

By default horta-web will use host's 80 port for web interface access. If you want to change this port, feel free to
modify `Vagrantfile`.

Updating the packages
---------------------

### Using the update script
This package designed to run the last stable horta-hell and horta-web versions (usually the ones from the `master`
branches of the corresponding repositories).

There are no default packages versions embedded with the image. All update actions should be taken manually by the
package administrator.

When started the virtual machine first time (or at any point when you're ready to update horta packages and restart
everything) connect to the machine with `ssh`:

    $ vagrant ssh

After that invoke the update script:

    $ sudo /vagrant/update-horta.sh

This script will take care of cloning or updating the source code of all embedded packages, (re-)compiling,
(re-)installing and (re-)starting the services.

### Manual update using the precompiled package
Another variant is compiling the packages on another machine and manual copying them to the vagrant box. To do it, first
compile the package (e.g using the `sbt clean assembly` command) and then copy it to
`/opt/codingteam/horta-hell/horta-hell.jar`). After that, restart the service:

    $ sudo restart horta-hell

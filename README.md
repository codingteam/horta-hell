horta-foundation
================
horta-foundation is a project for providing the default [horta-hell](https://github.com/codingteam/horta-hell)
environment ready for production.

It is a [Vagrant](http://www.vagrantup.com/) image for deployment horta-hell itself and
[horta-web](https://github.com/codingteam/horta-hell).  Vagrant is a virtual environment manager, horta-hell is an XMPP
bot, horta-web is a web interface for some its functions.

Prerequisites
-------------
Install the Vagrant package for your operation system. By default you will also need
[VirtualBox](https://www.virtualbox.org/). VirtualBox is just the default option; there is nothing in the
horta-foundation that has strict dependency on VirtualBox.

Configuring the Vagrant
-----------------------
Ypu may want to change Vagrant default forwarded SSH port. To do that, edit the `Vagrantfile`.

Install the `trusty64` image if yo have not already done it:

    $ vagrant box add ubuntu/trusty64

Install the [Berkshelf Vagrant plugin](https://github.com/berkshelf/vagrant-berkshelf):

    $ vagrant plugin install vagrant-berkshelf

After that, you can start the virtual machine at any moment with

    $ vagrant up

Stop it with

    $ vagrant halt

And destroy with

    $ vagrant destroy

All sensitive data such as horta database will be preserved at the host even after you destroy the image.

Configuring the horta
---------------------
You should create and place the `horta.properties` file inside the image directory. Please take a look at the
[horta documentation](https://github.com/codingteam/horta-hell) to know more about the configuration files. There is an
example file bundled with the horta-hell.

Please note that there should only be UNIX line endings (plain `\n`) in the `horta.properties` file!

Currently horta uses the access to host directory for parsing the chat logs.  This directory should be defined in the
host's `HORTA_LOGS` environment variable before starting the vagrant image. By default current directory will be mapped
if `HORTA_LOGS` is undefined.

There is an example of a simple start script `vagrant-up.ps1.example` for setting up the Vagrant environment.

Configuring the horta-web
-------------------------
You should place the `horta-web-app.conf` configuration file inside the image directory. Copy the initial
`conf/application.conf` file from the [horta-web](https://github.com/codingteam/horta-hell) package and change the paths
you want.

By default horta-web will use host's `8059` port for web interface access. If you want to change this port, feel free to
modify `Vagrantfile`.

Updating the packages
---------------------

### Automated deployment
The default and recommended option is to build the packages somewhere else and copy them to the following locations:
- `horta-hell` goes to `/opt/codingteam/horta-hell/horta-hell.jar`;
- `horta-web` main executable goes to `/opt/codingteam/horta-web/bin/horta-web`.

After you have deployed the packages, simply restart the corresponding services:

    $ sudo restart horta-hell
    $ sudo restart horta-web

### Example script for horta-hell redeployment
First, copy the `horta-hell-assembly.jar` file to the `/tmp` directory. Then,

    $ sudo mkdir -p /opt/codingteam/horta-hell
    $ sudo mv -f /tmp/horta-hell-assembly.jar /opt/codingteam/horta-hell/horta-hell.jar
    $ sudo restart horta-hell || sudo start horta-hell

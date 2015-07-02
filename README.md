horta-foundation
================
[![codingteam/horta-hell](http://issuestats.com/github/codingteam/horta-foundation/badge/pr?style=flat-square)](http://www.issuestats.com/github/codingteam/horta-foundation) [![codingteam/horta-hell](http://issuestats.com/github/codingteam/horta-foundation/badge/issue?style=flat-square)](http://www.issuestats.com/github/codingteam/horta-foundation)

horta-foundation is a project for providing the default [horta-hell][]
environment ready for production.

It is a [docker][] image for deployment of horta-hell itself and [horta-web][].
horta-hell is an XMPP bot, horta-web is a web interface for some of its
functions.

*NOTE*: horta-web is currently not bundled with the image. It will be enabled in
the future.

Prerequisites
-------------
You should only install docker (boot2docker is a viable option too).

Building the image
------------------
First, build the horta-hell according to its manual and copy it to
`horta-hell.jar` file in the current image directory. After that, issue command

    $ docker build -t=codingteam/horta-hell .

Data volumes
------------
You should mount the volume containing `horta.properties` file to the `/data`
mountpoint in the container.

Configuring the horta
---------------------
You should create and place the `horta.properties` file inside the `/data`
container directory. Please take a look at the [horta documentation][horta-hell]
to know more about the configuration files. There is an example file bundled
with the horta-hell.

Please note that there should only be UNIX line endings (plain `\n`) in the
`horta.properties` file!

Configuring the horta-web
-------------------------
*NOTE*: horta-web is currently not bundled with the image. It will be enabled in
the future. This section of Readme is outdated and will be updated.

You should place the `horta-web-app.conf` configuration file inside the image
directory. Copy the initial `conf/application.conf` file from the [horta-web][]
package and change the paths you want.

By default horta-web will use host's `8059` port for web interface access. If
you want to change this port, feel free to modify `Vagrantfile`.

Running the container
---------------------
Here's an example script for running the container. Windows users may be also
interested in `Run-Container.ps1` script.

    $ cd horta-foundation
    $ cp -f /path/to/build/horta-hell-assembly.jar ./horta-hell.jar
    $ docker build -t=codingteam/horta-hell .
    $ docker stop horta-hell # in case it already exists
    $ docker rm horta-hell
    $ docker run -d --name horta-hell -v /path/to/local/horta/configuration/directory:/data codingteam/horta-hell

Updating the packages
---------------------
The recommended way of upgrade is to [rebuild and recreate the whole
container][so-docker-upgrade]. Your data is stored on an external data volume,
so it will be fully preserved.

[docker]: https://www.docker.com/
[horta-hell]: https://github.com/codingteam/horta-hell
[horta-web]: https://github.com/codingteam/horta-web
[so-docker-upgrade]: http://stackoverflow.com/questions/26734402

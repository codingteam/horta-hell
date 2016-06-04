horta-foundation
================
[![codingteam/horta-hell](http://issuestats.com/github/codingteam/horta-foundation/badge/pr?style=flat-square)](http://www.issuestats.com/github/codingteam/horta-foundation) [![codingteam/horta-hell](http://issuestats.com/github/codingteam/horta-foundation/badge/issue?style=flat-square)](http://www.issuestats.com/github/codingteam/horta-foundation)

horta-foundation is a project for providing the default [horta-hell][]
environment ready for production.

It is a [docker][] image for deployment of horta-hell, an XMPP bot.

In the future, the image will also provide [horta-web][], a web interface to
some of horta's functions.

Prerequisites
-------------
You should only install docker (boot2docker is a viable option too).

Building the image
------------------
First, build the horta-hell according to its manual and copy it to
`horta-hell.jar` in the current image directory. After that, issue the command

    $ docker build -t=codingteam/horta-hell .

Data volumes
------------
You should mount the volume containing `horta.properties` to the `/data`
mountpoint in the container.

Configuring the horta
---------------------
You should create and place the `horta.properties` inside the `/data` container
directory. Please take a look at the [horta documentation][horta-hell] to know
more about the configuration files. There is an example file bundled with the
horta-hell.

Please use only UNIX line endings (plain `\n`) in the `horta.properties`!

Running the container
---------------------
Here's an example script for running the container. Windows users may also be
interested in `Run-Container.ps1` script.

    $ cd horta-foundation
    $ cp -f /path/to/build/horta-hell-assembly.jar ./horta-hell.jar
    $ docker build -t=codingteam/horta-hell .
    $ docker stop horta-hell # in case it already exists
    $ docker rm horta-hell
    $ docker run -d --name horta-hell -v /path/to/local/horta/configuration/directory:/data codingteam/horta-hell

### Configuring for Windows

Currently the recommended way of running the container on Windows is to deploy
it through [Docker for Windows][docker-for-windows].

It's recommended to execute `Run-Container.ps1` script like this (please check
the parameter section):

    $HortaArtifact = 'some\path\target\scala-2.11\horta-hell-assembly.jar'
    $VolumePath = 'c:/Users/UserName/Docker-Data/horta-hell'

    & horta-foundation\Run-Container.ps1 $HortaArtifact $VolumePath

After that you can run this script and have all the deployment process
automated.

Updating the packages
---------------------
The recommended way of upgrading is to [rebuild and recreate the whole
container][so-docker-upgrade]. Your data is stored on an external data volume,
so it will be fully preserved.

[docker]: https://www.docker.com/
[docker-for-windows]: https://beta.docker.com/docs/windows/
[horta-hell]: https://github.com/codingteam/horta-hell
[horta-web]: https://github.com/codingteam/horta-web
[so-docker-upgrade]: http://stackoverflow.com/questions/26734402

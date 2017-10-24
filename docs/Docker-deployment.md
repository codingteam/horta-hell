Docker deployment
=================

There's a [Docker][docker] environment provided for reliable production
deployment of horta-hell. This document describes how to deploy horta-hell into
a local Docker container.

If you want to publish your own Docker package, [consult the
documentation][docs/docker-publish].

Prerequisites
-------------

The only prerequisite is [Docker][docker] itself.

Building the image
------------------

First, build the horta-hell according to [the build instructions][manual/build]
its manual and copy the resulting `horta-hell.jar` into `src/docker` directory.
After that, issue the command

```console
$ docker build -t=codingteam/horta-hell .
```

Configuring horta-hell
----------------------

You should mount the volume containing `horta.properties` to the `/data`
mountpoint in the container.

Please take a look at the [documentation][manual] to know more about the
configuration files. There is an example file bundled with horta-hell.

⚠ Please use only UNIX line endings (plain `\n`) in the `horta.properties`!

It's recommended to store the database on the same `/data` volume.

Running the container
---------------------

Here's an example script for running the container.

```console
$ cd horta-foundation
$ cp -f /path/to/build/horta-hell-assembly.jar ./horta-hell.jar
$ docker build -t=codingteam/horta-hell .
$ docker stop horta-hell # in case it already exists
$ docker rm horta-hell
$ docker run -d --name horta-hell -v /path/to/local/horta/configuration/directory:/data codingteam/horta-hell
```

Updating the packages
---------------------

The recommended way of upgrading is to [rebuild and recreate the whole
container][so-docker-upgrade]. Make sure your data is stored on an external data
volume, so it's fully preserved on container rebuild!

[docs/docker-publish]: How-to-publish-Docker-image.md
[manual]: ../README.md
[manual/build]: ../README.md#building

[docker]: https://www.docker.com/
[so-docker-upgrade]: http://stackoverflow.com/questions/26734402

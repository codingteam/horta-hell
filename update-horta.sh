#!/bin/bash
HORTA_HOME=/opt/codingteam/horta-hell

if [ ! -d "$HORTA_HOME" ]; then
	mkdir -p "$HORTA_HOME"
fi

cd "$HORTA_HOME"

if [ ! -d "./.git" ]; then
	git clone https://github.com/codingteam/horta-hell.git .
fi

git pull

# Stop the daemon:
stop horta-hell

# Build the new version:
sbt one-jar
mv target/scala-2.10/*-one-jar.jar ./horta-hell.jar
sbt clean

cp /vagrant/horta.properties ./horta.properties

# Start the daemon:
start horta-hell

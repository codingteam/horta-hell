#!/bin/bash -x
HORTA_HOME=/opt/codingteam/horta-hell
HORTA_WEB_HOME=/opt/codingteam/horta-web

if [ ! -d "$HORTA_HOME" ]; then
	mkdir -p "$HORTA_HOME"
fi

cd "$HORTA_HOME"

if [ ! -d "./.git" ]; then
	git clone https://github.com/codingteam/horta-hell.git .
fi

git pull

sbt assembly
mv target/scala-2.11/horta-hell-assembly.jar ./horta-hell.jar
sbt clean

if [ ! -d "$HORTA_WEB_HOME" ]; then
	mkdir -p "$HORTA_WEB_HOME"
fi

cd "$HORTA_WEB_HOME"

if [ ! -d "./.git" ]; then
	git clone https://github.com/codingteam/horta-web.git .
fi

git pull

sbt dist
rm -r ./dist
mkdir ./dist
unzip ./target/universal/horta-web-1.0-SNAPSHOT.zip -d ./dist

cp /vagrant/horta-hell.conf /etc/init/
cp /vagrant/horta-web.conf /etc/init/
restart horta-hell
restart horta-web

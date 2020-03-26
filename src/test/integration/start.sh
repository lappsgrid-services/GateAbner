#!/usr/bin/env bash

target=`pwd`/target
echo $target
#docker run -d -p 8080:8080 --name abner -v $target:/var/lib/tomcat7/webapps -v /usr/local/lapps:/usr/local/lapps lappsgrid/tomcat7:1.2.3
docker run -d -p 8080:8080 --name abner -v $target:/var/lib/tomcat7/webapps -v /usr/local/lapps:/usr/local/lapps lappsgrid/tomcat7:1.2.3
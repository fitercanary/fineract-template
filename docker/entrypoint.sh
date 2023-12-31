#!/bin/bash

envsubst < /usr/local/tomcat/templates/server.xml.tmpl > /usr/local/tomcat/conf/server.xml
envsubst < /usr/local/tomcat/templates/setenv.sh.tmpl > /usr/local/tomcat/bin/setenv.sh
keytool -genkey -noprompt -storepass password -keypass password -keyalg RSA -alias tomcat -dname "CN=tomcat" -keystore /usr/share/tomcat.keystore

catalina.sh jpda run

#FROM tomcat:7.0.94-jre8-alpine
FROM tomcat:8.5.57-jdk8

#RUN apk add --no-cache mysql-client curl
RUN apt-get install curl gettext

#RUN apk --update add fontconfig ttf-dejavu ttf-liberation nano

#ADD ./docker/setenv.sh /usr/local/tomcat/bin/setenv.sh

#ADD ./docker/server.xml /usr/local/tomcat/conf/server.xml

ADD ./docker/mysql-connector-java-8.0.18.jar /usr/local/tomcat/lib/mysql-connector-java-8.0.18.jar

ADD ./build/libs/fineract-provider.war /usr/local/tomcat/webapps/fineract-provider.war

#ADD ./community-app/ /usr/local/tomcat/webapps/community-app
#ADD ./community-app/ /usr/local/tomcat/webapps/ROOT/
#ADD ./test/ /usr/local/tomcat/webapps/test/ 

RUN mkdir -p /app .mifosx /root/.mifosx 

ADD ./fineract-provider/src/main/pentahoReports/ /root/.mifosx/pentahoReports

#ADD ./reports/properties/ /root/.mifosx/properties

ADD ./docker/keystore.jks /usr/local/tomcat/keystore.jks

#ADD ./images/signature.png /root/.fineract/default/signature/signature.png

ADD templates/server.xml.tmpl /usr/local/tomcat/templates/server.xml.tmpl

ADD templates/setenv.sh.tmpl /usr/local/tomcat/templates/setenv.sh.tmpl

ADD ./docker/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

RUN mkdir -p /run/mysqld \
	&& chown 999 /run/mysqld

RUN ls -lrt /usr/local/tomcat/webapps/
#VOLUME /docker-entrypoint-initdb.d

ENTRYPOINT /entrypoint.sh

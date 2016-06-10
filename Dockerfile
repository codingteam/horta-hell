FROM java:8
MAINTAINER friedrich@fornever.me
RUN apt-get update && apt-get install -y supervisor

# Initialize LogList certificate:
RUN openssl s_client -showcerts -connect loglist.net:443 </dev/null 2>/dev/null\
    | openssl x509 -outform PEM >/tmp/loglist.net.pem
RUN keytool -importcert -noprompt \
    -file /tmp/loglist.net.pem \
    -keystore $JAVA_HOME/jre/lib/security/cacerts \
    -storepass changeit \
    -alias loglist.net
RUN rm /tmp/loglist.net.pem

COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY horta-hell.jar /opt/codingteam/horta-hell/horta-hell.jar
VOLUME ["/data"]
CMD ["/usr/bin/supervisord"]

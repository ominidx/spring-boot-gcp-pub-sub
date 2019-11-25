FROM openjdk:8-jdk-alpine
RUN apk update && apk add libc6-compat
#RUN apk update && apk add ca-certificates && rm -rf /var/cache/apk/*
#RUN update-ca-certificates
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY blogpostpoc.json blogpostpoc.json
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xms256M","-Xmx1024m","-jar","/app.jar"]
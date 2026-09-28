FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds

LABEL maintainer="cat2bug"

RUN mkdir -p /home/cat2bug/logs \
    /home/cat2bug/temp \
    /home/cat2bug/data \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && microdnf install -y procps ca-certificates wget curl

WORKDIR /home/cat2bug

ENV EXPOSE_SERVER_PORT=8000 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS="" TZ=Asia/Shanghai

EXPOSE ${EXPOSE_SERVER_PORT}

ADD ./cat2bug-platform-admin/target/cat2bug-admin.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${EXPOSE_SERVER_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError ${JAVA_OPTS} \
           -jar app.jar

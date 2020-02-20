FROM openjdk:11-jre-slim as builder
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    # seems that dash package upgrade is broken in Debian, so we hold it's version before update
    echo "dash hold" | dpkg --set-selections && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt wget

WORKDIR /root
RUN wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-19.3.1/graalvm-ce-java8-linux-amd64-19.3.1.tar.gz && \
    tar -xf graalvm-ce-java8-linux-amd64-19.3.1.tar.gz

ENV JAVA_HOME="/root/graalvm-ce-java8-19.3.1"
ENV PATH="${JAVA_HOME}/bin:$PATH"

COPY ["ergo-appkit/build.sbt", "/ergo-appkit/"]
COPY ["ergo-appkit/project", "/ergo-appkit/project"]
RUN sbt update

COPY ergo-appkit /ergo-appkit
WORKDIR /ergo-appkit
RUN sbt assembly

ENV JAVA_HOME="/usr/local/openjdk-11"
ENV PATH="${JAVA_HOME}/bin:$PATH"
COPY . /customverifier
RUN mkdir /customverifier/lib && cp -r /ergo-appkit/target/scala-2.12/* /customverifier/lib/
WORKDIR /customverifier
RUN sbt assembly
RUN mv `find . -name tx-verify-assembly*` /tx-verify.jar
CMD ["java", "-jar", "/tx-verify.jar"]

FROM openjdk:11-jre-slim
#LABEL maintainer="saber dashti <s.dashti@gmail.com>"
RUN adduser --disabled-password --home /home/ergo --uid 9052 --gecos "ErgoPlatform" ergo && \
    install -m 0750 -o ergo -g ergo  -d /home/ergo/customverifier
COPY --from=builder /tx-verify.jar /home/ergo/customverifier/tx-verify.jar
USER ergo
EXPOSE 9001
WORKDIR /home/ergo/customverifier
ENTRYPOINT java -jar /home/ergo/customverifier/tx-verify.jar
CMD [""]

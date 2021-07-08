FROM openjdk:8u292-slim
RUN apt-get -y update; \
    apt-get -y upgrade; \
    apt-get -y install apt-utils \
    vim \
    htop;
RUN apt-get -y install dstat
RUN apt-get -y install sudo
#RUN apt-get -y install apache2
RUN apt-get -y install nano
RUN apt-get -y install systemd
RUN apt-get install -y wget
#RUN apt-get update && apt-get install -y gnupg2
#RUN apt-get install -y software-properties-common
RUN wget https://downloads.apache.org/kafka/2.8.0/kafka_2.13-2.8.0.tgz
 
RUN tar xzf kafka_2.13-2.8.0.tgz
RUN mv kafka_2.13-2.8.0 /usr/local/kafka
 
#RUN wget -qO - https://packages.confluent.io/deb/6.2/archive.key | sudo apt-key add -
#RUN sudo add-apt-repository "deb [arch=amd64] https://packages.confluent.io/deb/6.2 stable main"
#RUN sudo apt-get update && sudo apt-get install -y confluent-community-2.13
#RUN apt-get install -y confluent-control-center
#RUN cp -R /usr/local/kafka/ /opt
#COPY "./kafka" "kafka"
#RUN mv kafka /etc/init.d/kafka
#RUN chmod 755 /etc/init.d/kafka
#RUN update-rc.d kafka defaults
#RUN /usr/bin/setsid /usr/local/kafka/bin/zookeeper-server-start.sh /usr/local/kafka/config/zookeeper.properties
#COPY "./zookeeper.service" "zookeeper.service"
#COPY "./kafka.service" "kafka.service"
 
#RUN mv zookeeper.service /etc/systemd/system/zookeeper.service
#RUN mv kafka.service /etc/systemd/system/kafka.service
#RUN /usr/bin/setsid /usr/local/kafka/bin/kafka-server-start.sh /usr/local/kafka/config/server.properties 
EXPOSE 9092
EXPOSE 2181
#CMD [ "bash" ] && ["service","apache2","start"]
ENTRYPOINT ["/usr/local/kafka/bin/zookeeper-server-start.sh","/usr/local/kafka/config/zookeeper.properties" ]
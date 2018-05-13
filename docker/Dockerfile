FROM ubuntu:16.04

USER root

# install dev tools
RUN apt-get update
RUN apt-get install -y curl tar sudo openssh-server openssh-client rsync dos2unix

# passwordless ssh
RUN rm -f /etc/ssh/ssh_host_dsa_key /etc/ssh/ssh_host_rsa_key /root/.ssh/id_rsa
RUN ssh-keygen -q -N "" -t dsa -f /etc/ssh/ssh_host_dsa_key
RUN ssh-keygen -q -N "" -t rsa -f /etc/ssh/ssh_host_rsa_key
RUN ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa
RUN cp /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys


RUN apt-get install openjdk-8-jdk -y
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

# hadoop
RUN cd /usr/local && \
    wget http://apache-mirror.rbc.ru/pub/apache/hadoop/common/hadoop-2.7.6/hadoop-2.7.6.tar.gz && \
    tar xzfv hadoop-2.7.6.tar.gz && \
    ln -s ./hadoop-2.7.6 hadoop

ENV HADOOP_PREFIX /usr/local/hadoop
RUN sed -i '/^export JAVA_HOME/ s:.*:export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64\nexport HADOOP_PREFIX=/usr/local/hadoop\nexport HADOOP_HOME=/usr/local/hadoop\n:' $HADOOP_PREFIX/etc/hadoop/hadoop-env.sh
RUN sed -i '/^export HADOOP_CONF_DIR/ s:.*:export HADOOP_CONF_DIR=/usr/local/hadoop/etc/hadoop/:' $HADOOP_PREFIX/etc/hadoop/hadoop-env.sh
RUN . $HADOOP_PREFIX/etc/hadoop/hadoop-env.sh

RUN mkdir $HADOOP_PREFIX/input
RUN cp $HADOOP_PREFIX/etc/hadoop/*.xml $HADOOP_PREFIX/input

# pseudo distributed
ADD core-site.xml.template $HADOOP_PREFIX/etc/hadoop/core-site.xml.template
RUN dos2unix $HADOOP_PREFIX/etc/hadoop/core-site.xml.template
RUN sed s/HOSTNAME/localhost/ /usr/local/hadoop/etc/hadoop/core-site.xml.template > /usr/local/hadoop/etc/hadoop/core-site.xml
ADD hdfs-site.xml $HADOOP_PREFIX/etc/hadoop/hdfs-site.xml
RUN dos2unix $HADOOP_PREFIX/etc/hadoop/hdfs-site.xml

ADD mapred-site.xml $HADOOP_PREFIX/etc/hadoop/mapred-site.xml
RUN dos2unix $HADOOP_PREFIX/etc/hadoop/mapred-site.xml
ADD yarn-site.xml $HADOOP_PREFIX/etc/hadoop/yarn-site.xml
RUN dos2unix $HADOOP_PREFIX/etc/hadoop/yarn-site.xml


RUN $HADOOP_PREFIX/bin/hdfs namenode -format

# fixing the libhadoop.so like a boss
#RUN rm  /usr/local/hadoop/lib/native/*
#RUN curl -Ls http://dl.bintray.com/sequenceiq/sequenceiq-bin/hadoop-native-64-2.6.0.tar|tar -x -C /usr/local/hadoop/lib/native/

ADD ssh_config /root/.ssh/config
RUN dos2unix /root/.ssh/config
RUN chmod 600 /root/.ssh/config
RUN chown root:root /root/.ssh/config

ADD bootstrap.sh /etc/bootstrap.sh
RUN dos2unix /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

ENV BOOTSTRAP /etc/bootstrap.sh

# workingaround docker.io build error
RUN ls -la /usr/local/hadoop/etc/hadoop/*-env.sh
RUN chmod +x /usr/local/hadoop/etc/hadoop/*-env.sh
RUN ls -la /usr/local/hadoop/etc/hadoop/*-env.sh

# fix the 254 error code
RUN sed  -i "/^[^#]*UsePAM/ s/.*/#&/"  /etc/ssh/sshd_config
RUN echo "UsePAM no" >> /etc/ssh/sshd_config
RUN echo "Port 2122" >> /etc/ssh/sshd_config

RUN echo localhost > /etc/hostname


RUN service ssh start && $HADOOP_PREFIX/etc/hadoop/hadoop-env.sh && $HADOOP_PREFIX/sbin/start-dfs.sh && $HADOOP_PREFIX/bin/hdfs dfs -mkdir -p /user/root
RUN service ssh start && $HADOOP_PREFIX/etc/hadoop/hadoop-env.sh && $HADOOP_PREFIX/sbin/start-dfs.sh && $HADOOP_PREFIX/bin/hdfs dfs -put $HADOOP_PREFIX/etc/hadoop/ input

#CMD ["/etc/bootstrap.sh", "-d"]

ENV TERM linux
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get install less -y
RUN apt-get install man -y
RUN apt-get install vim -y
RUN apt-get install telnet -y
RUN apt-get install unzip -y

RUN cd /opt && \
    wget  http://apache-mirror.rbc.ru/pub/apache/spark/spark-2.3.0/spark-2.3.0-bin-hadoop2.7.tgz && \
    tar xzfv spark-2.3.0-bin-hadoop2.7.tgz && \
    wget http://apache-mirror.rbc.ru/pub/apache/incubator/livy/0.5.0-incubating/livy-0.5.0-incubating-bin.zip && \
    unzip livy-0.5.0-incubating-bin.zip && \
    mkdir /opt/livy-0.5.0-incubating-bin/logs && \
    echo livy.file.local-dir-whitelist=/ > /opt/livy-0.5.0-incubating-bin/conf/livy.conf && \
    echo export SPARK_HOME=/opt/spark-2.3.0-bin-hadoop2.7 > livy-starter.sh && \
    echo /opt/livy-0.5.0-incubating-bin/bin/livy-server >> livy-starter.sh && \
    chmod a+x livy-starter.sh


EXPOSE 50020 50090 50070 50010 50075 8031 8032 8033 8040 8042 49707 22 8088 8030

ENTRYPOINT ["/bin/bash"]

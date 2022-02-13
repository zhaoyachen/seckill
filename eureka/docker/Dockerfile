#FROM：指定基础镜像，必备的指令，并且必须是第一条指令
FROM openjdk:8
#MAINTAINER：指明该镜像的维护者和电子邮件
#MAINTAINER xxx "xxx@qq.com"
#ENV:设置环境变量
#ENV key1=value1 key2=value2
#RUN:在新镜像内部执行的命令，比如安装一些软件，配置一些基础环境，可使用\来换行
#RUN yum install -y mysql-server
#WORKDIR：在构建镜像时，指定镜像的工作目录，之后的命令都是基于此工作目录，如果目录不存在，会自动创建目录，而且在进入容器时，会默认定位到该路径下
WORKDIR /opt/software/docker-compose
#COPY:将主机的文件复制到镜像文件中，如果目标位置不存在会自动创建
COPY eureka.jar ./
#ADD:和COPY一样，但是ADD会加上解压操作
#EXPOSE：暴露镜像的端口供主机做映射，可以暴露多个端口
EXPOSE 20000
#VOLUME:用来向基础镜像中添加数据卷
#VOLUME /root/mydata /root/condata
#CMD:容器启动时需要执行的命令
CMD ['java','-jar','eureka.jar']

FROM maven:3.5-jdk-8 AS build

# copy your source tree
COPY server /home/amoeba/server

# build for release
RUN mvn -f /home/amoeba/server/ clean package

FROM openjdk:8

RUN mkdir -p /home/amoeba/db
RUN mkdir -p /home/amoeba/log
COPY client /home/amoeba/client

COPY --from=build /home/amoeba/server/web/target/amoeba-jar-with-dependencies.jar /usr/local/lib/amoeba.jar
CMD java -jar /usr/local/lib/amoeba.jar -c /home/amoeba/client/ -l /home/amoeba/log -d /home/amoeba/db -p $PORT

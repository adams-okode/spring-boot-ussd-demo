# Start with a base image containing Java runtime
FROM maven:3.6.3-jdk-8-slim AS build

COPY src /usr/src/app/src

COPY pom.xml /usr/src/app

RUN mvn -f /usr/src/app/pom.xml clean package


FROM openjdk:8-jdk-alpine

# Make port 8080 available to the world outside this container
EXPOSE 8080 

COPY --from=build /usr/src/app/target/ussd-0.0.1-SNAPSHOT.jar /usr/app/target/ussd-0.0.1-SNAPSHOT.jar

# Run the jar file 
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/usr/app/target/ussd-0.0.1-SNAPSHOT.jar"]
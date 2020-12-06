# Start with a base image containing Java runtime
FROM openjdk:8-jdk-alpine

# Add Maintainer Info
LABEL maintainer="aokode@eedadvisory.com"

# Add a volume pointing to /tmpe
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080 
EXPOSE 8996

# The application's jar file
ARG JAR_FILE=target/core-0.0.1.jar

# Add the application's jar to the container
ADD ${JAR_FILE} core.jar

# Run the jar file 
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/core.jar"]
FROM openjdk:11-ea-jre
VOLUME /tmp
COPY build/libs/releaser-0.0.1-SNAPSHOT.jar Releaser.jar
ENTRYPOINT ["java", "-jar", "Releaser.jar"]
FROM eclipse-temurin:23-jdk-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=prod"]
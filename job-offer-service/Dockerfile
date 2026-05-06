FROM eclipse-temurin:17-jre

WORKDIR /app

# Jenkins package stage creates the runnable Spring Boot jar in target/
ARG JAR_FILE=target/job-offer-service-*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

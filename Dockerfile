FROM amazoncorretto:21-alpine3.20

COPY build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]

FROM amazoncorretto:21-alpine3.16

COPY build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]

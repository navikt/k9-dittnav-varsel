FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:latest

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

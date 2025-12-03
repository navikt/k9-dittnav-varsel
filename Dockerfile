FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2025.12.03.1527Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

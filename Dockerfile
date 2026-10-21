FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.10.21.1442Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

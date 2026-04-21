FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.21.0818Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

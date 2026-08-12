FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.08.12.1516Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.08.24.0750Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

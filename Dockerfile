FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.16.0737Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

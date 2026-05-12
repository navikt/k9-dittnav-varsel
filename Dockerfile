FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.05.11.0700Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

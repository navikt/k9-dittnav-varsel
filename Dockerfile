FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.26.0840Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

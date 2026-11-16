FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.13.0858Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

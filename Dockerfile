FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.08.31.0909Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

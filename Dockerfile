FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.05.07.0728Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.03.16.1330Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

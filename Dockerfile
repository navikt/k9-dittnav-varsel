FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.10.19.1216Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

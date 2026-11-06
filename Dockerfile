FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.06.1317Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

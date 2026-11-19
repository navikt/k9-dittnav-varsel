FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.19.0715Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.05.19.1046Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.08.03.0816Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

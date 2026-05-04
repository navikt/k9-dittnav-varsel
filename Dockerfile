FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.30.1354Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

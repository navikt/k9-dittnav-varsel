FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.09.02.1329Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

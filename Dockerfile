FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.02.06.0908Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

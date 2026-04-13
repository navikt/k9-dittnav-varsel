FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.13.0727Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

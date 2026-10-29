FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.10.29.0903Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

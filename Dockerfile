FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.10.26.0916Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

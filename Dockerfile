FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.01.13.1201Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

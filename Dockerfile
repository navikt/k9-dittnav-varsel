FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.09.0747Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

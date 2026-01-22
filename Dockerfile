FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.01.22.0755Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

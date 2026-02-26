FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.02.26.1259Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

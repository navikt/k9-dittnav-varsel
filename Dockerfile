FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.07.02.1354Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

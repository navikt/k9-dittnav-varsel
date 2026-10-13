FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.10.12.1155Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

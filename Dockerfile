FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.07.27.1137Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

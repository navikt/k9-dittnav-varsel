FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.09.01.1219Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]

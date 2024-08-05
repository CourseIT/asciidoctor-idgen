FROM eclipse-temurin:17-jdk-alpine
VOLUME /doc
COPY target/asciidoctor-idgen-jar-with-dependencies.jar /asciidoctor-idgen.jar
COPY docker/idgen /idgen
ENV PATH="/:${PATH}"
RUN chmod +x /idgen

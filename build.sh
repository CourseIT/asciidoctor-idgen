./mvnw clean package
rm -rf ghpages && mkdir ghpages
cp target/asciidoctor-idgen-jar-with-dependencies.jar ghpages
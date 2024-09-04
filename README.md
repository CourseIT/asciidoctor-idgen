# asciidoctor-idgen
Parse .adoc files and generate ids


## Project artifacts

[Current jar](https://courseit.github.io/asciidoctor-idgen/asciidoctor-idgen-jar-with-dependencies.jar)

[Old jar](https://github.com/CourseIT/asciidoctor-idgen/blob/master/old-jar/asciidoctor-idgen-jar-with-dependencies.jar)

To build image: 

```
docker build . -t curs/asciidoctor-idgen
```

To run image:

```
docker run --rm -w /doc/[workdir] -v $PWD:/doc curs/asciidoctor-idgen idgen [params] 
```

To run image as a serve :

```
docker run --rm -w /doc/[workdir] -v $PWD:/doc curs/asciidoctor-idgen serveidgen 
```

Access service via `/api/enrich` path.

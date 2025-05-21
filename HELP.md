# Read Me First
The following was discovered as part of building this project:

* The JVM level was changed from '23' to '21' as the Kotlin version does not support Java 23 yet.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.3/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.4.3/gradle-plugin/packaging-oci-image.html)
* [Distributed Tracing Reference Guide](https://docs.micrometer.io/tracing/reference/index.html)
* [Getting Started with Distributed Tracing](https://docs.spring.io/spring-boot/3.4.3/reference/actuator/tracing.html)
* [Docker Compose Support](https://docs.spring.io/spring-boot/3.4.3/reference/features/dev-services.html#features.dev-services.docker-compose)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.3/reference/web/servlet.html)
* [Liquibase Migration](https://docs.spring.io/spring-boot/3.4.3/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Using Apache Camel with Spring Boot](https://camel.apache.org/camel-spring-boot/latest/spring-boot.html)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* postgres: [`postgres:latest`](https://hub.docker.com/_/postgres)

Please review the tags of the used images and set them to the same as you're running in production.

### Service
http://localhost:8001/ OpenZaak
http://localhost:8010/ Objects API
http://localhost:8011/ Objecttypes API

### Docker container steps:
* Building
```./gradlew bootBuildImage --imageName=iko-app```
or (then image name will be docker.io/library/iko:X.X.X-SNAPSHOT)
```./gradlew bootBuildImage```
* Running: 
```docker run --env-file .env -p 8080:8080 iko-app```
or
```docker run --env-file .env -p 8080:8080 docker.io/library/iko:0.0.1-SNAPSHOT```

The env.template file contains a SPRING_THYMELEAF_PREFIX=file:src/main/resources/templates/
This is to allow local dev to have no caching when working on HTML. Remove it when running the docker container.

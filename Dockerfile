FROM amazoncorretto:21 AS build

WORKDIR /app

COPY . /app

RUN chmod +x /app/gradlew
RUN ./gradlew :build

# also set platform for final image
FROM amazoncorretto:21

WORKDIR /app

# OCI image description label
LABEL org.opencontainers.image.description="Integraal Klant & Objectbeeld (IKO) application container image"

COPY --from=build /app/build/libs/iko.jar /app/iko.jar

ENTRYPOINT ["java", \
    "-agentlib:jdwp=transport=dt_socket,server=y,address=*:8000,suspend=n", \
    "-XX:InitialHeapSize=1024M", \
    "-XX:MinRAMPercentage=70", \
    "-XX:MaxRAMPercentage=80", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/iko.jar"]
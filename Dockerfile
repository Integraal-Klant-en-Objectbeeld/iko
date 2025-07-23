FROM amazoncorretto:21 AS build

WORKDIR /app

COPY ./build.gradle.kts /app/build.gradle.kts
COPY ./settings.gradle.kts /app/settings.gradle.kts
COPY ./gradlew /app/gradlew
COPY ./gradle /app/gradle
COPY ./src /app/src

RUN chmod +x /app/gradlew
RUN ./gradlew :build

FROM amazoncorretto:21

WORKDIR /app

COPY --from=build /app/build/libs/iko-0.0.1-SNAPSHOT.jar /app/iko.jar

ENTRYPOINT ["java", \
    "-agentlib:jdwp=transport=dt_socket,server=y,address=*:8000,suspend=n", \
    "-XX:InitialHeapSize=1024M", \
    "-XX:MinRAMPercentage=70", \
    "-XX:MaxRAMPercentage=80", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/iko.jar"]
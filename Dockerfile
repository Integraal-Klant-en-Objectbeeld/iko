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
COPY --from=build --chmod=775 /app/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
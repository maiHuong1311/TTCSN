FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 spring \
    && useradd --uid 10001 --gid spring --no-create-home spring \
    && mkdir -p /app/uploads \
    && chown spring:spring /app/uploads

COPY --chown=spring:spring target/fashion-store-backend-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

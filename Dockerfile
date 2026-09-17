FROM node:22-bookworm-slim AS assets
WORKDIR /source
COPY package.json package-lock.json tailwind.config.cjs ./
COPY frontend ./frontend
COPY src/main/resources/templates ./src/main/resources/templates
COPY src/main/resources/static/js ./src/main/resources/static/js
RUN npm ci --ignore-scripts --no-audit --no-fund && npm run build

FROM gradle:8.13-jdk17 AS build
WORKDIR /source
COPY build.gradle ./
COPY src ./src
COPY --from=assets /source/src/main/resources/static/css ./src/main/resources/static/css
COPY --from=assets /source/src/main/resources/static/vendor ./src/main/resources/static/vendor
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app \
    && mkdir -p /app /data/images && chown -R app:app /app /data/images
WORKDIR /app
COPY --from=build --chown=app:app /source/build/libs/*.jar /app/site.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/site.jar"]

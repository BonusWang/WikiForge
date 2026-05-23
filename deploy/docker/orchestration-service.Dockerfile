FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/wikiforge-common/pom.xml backend/wikiforge-common/pom.xml
COPY backend/wikiforge-core-service/pom.xml backend/wikiforge-core-service/pom.xml
COPY backend/wikiforge-worker-service/pom.xml backend/wikiforge-worker-service/pom.xml
COPY backend/wikiforge-orchestration-service/pom.xml backend/wikiforge-orchestration-service/pom.xml
RUN mvn -B -f backend/pom.xml -pl wikiforge-orchestration-service -am dependency:go-offline
COPY backend backend
RUN mvn -B -f backend/pom.xml -pl wikiforge-orchestration-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /workspace/backend/wikiforge-orchestration-service/target/wikiforge-orchestration-service-*.jar /app/wikiforge-orchestration-service.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/wikiforge-orchestration-service.jar"]

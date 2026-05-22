FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/wikiforge-common/pom.xml backend/wikiforge-common/pom.xml
COPY backend/wikiforge-core-service/pom.xml backend/wikiforge-core-service/pom.xml
COPY backend/wikiforge-worker-service/pom.xml backend/wikiforge-worker-service/pom.xml
RUN mvn -B -f backend/pom.xml -pl wikiforge-worker-service -am dependency:go-offline
COPY backend backend
RUN mvn -B -f backend/pom.xml -pl wikiforge-worker-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /workspace/backend/wikiforge-worker-service/target/wikiforge-worker-service-*.jar /app/wikiforge-worker-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/wikiforge-worker-service.jar"]

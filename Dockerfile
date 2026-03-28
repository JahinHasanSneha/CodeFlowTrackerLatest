# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn package -pl server -am -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/server/target/codeflow-server.jar ./server.jar
EXPOSE 8080
CMD ["java", "-jar", "server.jar"]

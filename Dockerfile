# # Build stage
# FROM maven:3.9-eclipse-temurin-17 AS build
# WORKDIR /app
# COPY . .
# RUN mvn package -pl server -am -DskipTests

# # Run stage
# FROM eclipse-temurin:17-jre
# WORKDIR /app
# COPY --from=build /app/server/target/codeflow-server.jar ./server.jar
# EXPOSE 8080
# CMD ["java", "-jar", "server.jar"]
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl server -DskipTests  # Ensure that the fat JAR is built

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the fat JAR from the build stage (adjust the JAR name here)
COPY --from=build /app/server/target/codeflow-server-1.0.0.jar ./server.jar

# Expose the port your application will run on
EXPOSE 8080

# Run the JAR file using the "java -jar" command
CMD ["java", "-jar", "server.jar"]

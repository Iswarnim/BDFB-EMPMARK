# Step 1: Build the application using Maven and Temurin JDK 17
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Use the stable Eclipse Temurin JRE image to run the app
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# This line finds the generated JAR and renames it to app.jar
COPY --from=build /app/target/*.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

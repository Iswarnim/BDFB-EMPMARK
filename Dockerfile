# Step 1: Use Maven to build the app
FROM maven:3.8.4-openjdk-17 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

# Step 2: Use a lightweight Java image to run it
FROM openjdk:17-jdk-slim
COPY --from=build /app/target/empmark-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

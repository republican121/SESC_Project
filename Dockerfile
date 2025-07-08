# Use Maven to build the project first
FROM maven:3.9.4-eclipse-temurin-17 as builder

WORKDIR /app

# Copy source code
COPY pom.xml .
COPY src ./src

# Build the JAR (skip tests to speed up)
RUN mvn clean package -DskipTests

# Use OpenJDK runtime for the final image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]

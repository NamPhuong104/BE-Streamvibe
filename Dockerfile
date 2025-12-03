# STAGE 1: Build app
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

#Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

#Copy source code
COPY src ./src

#Build app (skip tests for fast build)
RUN mvn clean package -DskipTests

#STAGE 2: Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

#Copy file JAR from build stage
COPY --from=build /app/target/movieapp-0.0.1-SNAPSHOT.jar app.jar

#Crete user non-root for run app (for more security)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

#Expose port
EXPOSE 8080

#Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

#Run app
ENTRYPOINT ["java", "-jar", "app.jar"]
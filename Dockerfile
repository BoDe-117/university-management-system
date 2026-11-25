FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
COPY src/ src/
RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

COPY --from=build /app/target/university-management-system-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

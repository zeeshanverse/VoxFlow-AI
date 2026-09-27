FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY mvnw.cmd .
RUN chmod +x mvnw.cmd 2>/dev/null || true
# Render/Linux uses the Maven wrapper only if it can execute a POSIX wrapper.
# For container portability, install Maven in the image.
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/voxflow-assistant-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -jar app.jar"]

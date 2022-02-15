FROM openjdk:17.0.2-jdk-slim
WORKDIR application
COPY . .
RUN ./gradlew --no-daemon installDist
CMD ["./build/install/auth-service/bin/auth-service"]
EXPOSE 8080
FROM openjdk:17.0.2-jdk-slim
WORKDIR application
COPY ./build/install/auth-service/bin/ .
#RUN ./gradlew --no-daemon installDist
CMD ["./auth-service"]
EXPOSE 8080
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/brujula-1.0.0.jar /app/brujula.jar
EXPOSE 8091
ENTRYPOINT ["java","-Duser.timezone=America/Lima","-jar","/app/brujula.jar"]

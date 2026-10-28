FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Dependencias en una capa propia: si solo cambia el código no se vuelven a descargar.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# docker build --target test . ejecuta los tests sin necesitar Java en la máquina.
FROM build AS test
RUN ./mvnw -B test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]

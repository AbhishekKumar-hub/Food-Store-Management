FROM openjdk:17

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/*.jar"]
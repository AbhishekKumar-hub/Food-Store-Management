FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

# give permission
RUN chmod +x mvnw

# build project
RUN ./mvnw clean package -DskipTests

# find jar and run it properly
CMD java -jar $(ls target/*.jar | head -n 1)
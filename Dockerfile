# STAGE 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Устанавливаем рабочую директорию
WORKDIR /build

# Копируем pom.xml и скачиваем зависимости (этот слой будет кэшироваться)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем приложение
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Create a slim final image with only the JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем собранный JAR-файл из стадии 'build'
COPY --from=build /build/target/*.jar app.jar

# Эта переменная будет использована для передачи профиля Spring
ENV SPRING_PROFILES_ACTIVE=docker

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
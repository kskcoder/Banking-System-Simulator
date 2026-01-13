# ----------- BUILD STAGE -----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 1️⃣ Build & install bankingcommon (local dependency)
COPY bankingcommon/pom.xml bankingcommon/pom.xml
COPY bankingcommon/src bankingcommon/src
RUN mvn -f bankingcommon/pom.xml clean install -DskipTests

# 2️⃣ Build Config Service
COPY bankconfigservice/pom.xml bankconfigservice/pom.xml
RUN mvn -f bankconfigservice/pom.xml dependency:go-offline

COPY bankconfigservice/src bankconfigservice/src
RUN mvn -f bankconfigservice/pom.xml clean package -DskipTests


# ----------- RUNTIME STAGE -----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/bankconfigservice/target/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java","-jar","app.jar"]

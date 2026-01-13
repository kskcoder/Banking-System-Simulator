# ----------- BUILD STAGE -----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 1️⃣ Build & install bankingcommon (local dependency)
COPY bankingcommon/pom.xml bankingcommon/pom.xml
COPY bankingcommon/src bankingcommon/src
RUN mvn -f bankingcommon/pom.xml clean install -DskipTests

# 2️⃣ Build Account Service
COPY bankaccountservice/pom.xml bankaccountservice/pom.xml
RUN mvn -f bankaccountservice/pom.xml dependency:go-offline

COPY bankaccountservice/src bankaccountservice/src
RUN mvn -f bankaccountservice/pom.xml clean package -DskipTests


# ----------- RUNTIME STAGE -----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/bankaccountservice/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java","-jar","app.jar"]

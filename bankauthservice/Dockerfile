# ----------- BUILD STAGE -----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 1️⃣ Build & install bankingcommon (local dependency)
COPY bankingcommon/pom.xml bankingcommon/pom.xml
COPY bankingcommon/src bankingcommon/src
RUN mvn -f bankingcommon/pom.xml clean install -DskipTests

# 2️⃣ Build Auth Service
COPY bankauthservice/pom.xml bankauthservice/pom.xml
RUN mvn -f bankauthservice/pom.xml dependency:go-offline

COPY bankauthservice/src bankauthservice/src
RUN mvn -f bankauthservice/pom.xml clean package -DskipTests


# ----------- RUNTIME STAGE -----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/bankauthservice/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

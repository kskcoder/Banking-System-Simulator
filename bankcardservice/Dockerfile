# ----------- BUILD STAGE -----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 1️⃣ Build & install bankingcommon (local dependency)
COPY bankingcommon/pom.xml bankingcommon/pom.xml
COPY bankingcommon/src bankingcommon/src
RUN mvn -f bankingcommon/pom.xml clean install -DskipTests

# 2️⃣ Build Card Service
COPY bankcardservice/pom.xml bankcardservice/pom.xml
RUN mvn -f bankcardservice/pom.xml dependency:go-offline

COPY bankcardservice/src bankcardservice/src
RUN mvn -f bankcardservice/pom.xml clean package -DskipTests


# ----------- RUNTIME STAGE -----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/bankcardservice/target/*.jar app.jar

EXPOSE 8095

ENTRYPOINT ["java","-jar","app.jar"]

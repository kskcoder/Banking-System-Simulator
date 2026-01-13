# ----------- BUILD STAGE -----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 1️⃣ Build & install bankingcommon (local dependency)
COPY bankingcommon/pom.xml bankingcommon/pom.xml
COPY bankingcommon/src bankingcommon/src
RUN mvn -f bankingcommon/pom.xml clean install -DskipTests

# 2️⃣ Build API Gateway
COPY bankapigateway/pom.xml bankapigateway/pom.xml
RUN mvn -f bankapigateway/pom.xml dependency:go-offline

COPY bankapigateway/src bankapigateway/src
RUN mvn -f bankapigateway/pom.xml clean package -DskipTests


# ----------- RUNTIME STAGE -----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/bankapigateway/target/*.jar app.jar

EXPOSE 8765

ENTRYPOINT ["java","-jar","app.jar"]

# ----------- BUILD STAGE -----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 1️⃣ Build & install bankingcommon (local dependency)
COPY bankingcommon/pom.xml bankingcommon/pom.xml
COPY bankingcommon/src bankingcommon/src
RUN mvn -f bankingcommon/pom.xml clean install -DskipTests

# 2️⃣ Build Service Registry
COPY bankserviceregistry/pom.xml bankserviceregistry/pom.xml
RUN mvn -f bankserviceregistry/pom.xml dependency:go-offline

COPY bankserviceregistry/src bankserviceregistry/src
RUN mvn -f bankserviceregistry/pom.xml clean package -DskipTests


# ----------- RUNTIME STAGE -----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/bankserviceregistry/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java","-jar","app.jar"]
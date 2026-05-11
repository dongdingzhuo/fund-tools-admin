# 1. 构建阶段：Maven打包Jar
FROM maven:3.9.6-eclipse-temurin-11 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 2. 运行阶段：极简JRE镜像
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
# 复制打包好的Jar
COPY --from=builder /app/target/*.jar app.jar
# 暴露端口
EXPOSE 8080
# 适配Render端口环境变量
ENTRYPOINT ["java", "-jar", "app.jar"]

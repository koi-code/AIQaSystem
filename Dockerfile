# ==========================================
# 第一阶段：编译构建阶段 (Builder)
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 1. 先缓存依赖（只要 pom.xml 没变，这步瞬间完成，极大提升打包速度）
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. 拷贝源码并打包
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# 第二阶段：运行阶段 (Runner)
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 1. 从构建阶段拷贝编译好的 jar 包
COPY --from=builder /build/target/*.jar app.jar

# 2. 【核心点】：将项目根目录下的离线词表整个拷入容器的 /app/tokenizers/
# 这样容器内的相对路径就和 IDEA 本地开发时一模一样了！
COPY tokenizers/ /app/tokenizers/

# 3. 设置时区为亚洲/上海
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
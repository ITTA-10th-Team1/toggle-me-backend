# 빌드 컨텍스트는 build/libs다: ./gradlew bootJar && docker build -f Dockerfile build/libs
# build/libs에 jar가 하나만 있어야 한다(./gradlew build가 만드는 -plain.jar가 있으면 COPY가 실패한다)
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY *.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

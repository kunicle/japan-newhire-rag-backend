FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"

COPY app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
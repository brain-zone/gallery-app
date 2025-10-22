FROM maven:3.6.3-jdk-8 AS build
COPY . /app
WORKDIR /app
RUN mvn -B -q clean package -DskipTests

FROM tomcat:8.5-jre8-alpine
RUN rm -rf /usr/local/tomcat/webapps.dist/* /usr/local/tomcat/webapps/*
# Copy your WAR to ROOT
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
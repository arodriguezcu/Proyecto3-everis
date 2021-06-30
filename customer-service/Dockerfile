FROM openjdk:8-alpine
COPY "./target/customer-service-0.0.1-SNAPSHOT.jar" "appcustomer-service.jar"
EXPOSE 8090
ENTRYPOINT ["java","-jar","appcustomer-service.jar"]
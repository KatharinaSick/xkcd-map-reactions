# xkcd-map-reactions
Just a small fun tool that maps sentences to routes, that can be displayed on maps. It is based on https://xkcd.com/2260/. 

Each folder in this repository contains a part of the implementation which is described below. 

## backend
The backend is a serverless function, to be precise an AWS Lambda, which should use an API Gateway as proxy. It is rather simple and does the whole work of mapping a passed phrase to a list of places, that is returned as JSON. A place simply contains its name, latitude and longitude. 

To run the function locally [AWS SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-test-and-debug.html) can be used. For IntelliJ users the [AWS Toolkit for JetBrains](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html) can be quite useful as well. Note that you need to set the following environment variables:

* DB_URL	
* DB_NAME
* DB_USER
* DB_PASSWORD

## dbMigration
This part of the project can be used to set up the database. It is a Java project that is using the [Flyway Maven Plugin](https://flywaydb.org/getstarted/firststeps/maven) to create database migrations. There are several steps you need to complete, to get it running:

1. In the pom.xml file, you need to enter your database url, user and password in the configuration section of the flyway-maven-plugin. The url should look like this: jdbc:postgresql://\<host\>:\<port\>/\<database\>
2. You need to download the US.zip file from http://download.geonames.org/export/dump/, extract it, and place the US.txt file in the resources folder (src/main/resources/). Unfortunately the file is too large to be pushed to git.
3. Run mvn compile
4. Check if everything is ok with mvn flyway:info and then migrate everything with mvn flyway:migrate. Depending on your machine and database this can take up to 10 hours, but if everything is local it should complete within 2 or 3 hours.

## frontend
The frontend is not implemented yet, but will follow in the next weeks.

## Contributors

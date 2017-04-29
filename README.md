# DATAC

![Build Status](https://apps.xlrnet.org/teamcity/app/rest/builds/buildType:(id:Datac_ContinuousBuild)/statusIcon) ![Quality Gate Status](https://apps.xlrnet.org/sonarqube/api/badges/gate?key=Datac)

Managing database changes has been a problem for a long time. While application source code was often maintained in a
code repository, databases were usually lacking such a mechanism. That's why configuration management tools like
[Liquibase](https://github.com/liquibase/liquibase) and [Flyway](https://github.com/flyway/flyway) were invented.

While database configuration management solutions are usually just command line based, Datac is going to provide you a
convenient web user interface for managing various instances and versions of databases based on the tool you like.
Liquibase, Flyway, raw SQL files or maybe your own in-house solution? 

Datac will allow you to deploy database changes to various stages, compare them or generate ready-to-run SQL files that
you can use as part of your documentation. By tracking changes in your source code repository, you can always generate
and deploy the correct version of your database for each revision of your source code.

(It supports only liquibase as of now)
  
## Running from source

1. Compile the application by running `mvn clean package`
2. Start a local H2 database with default settings (or download it from [here](http://www.h2database.com/html/download.html) if you don't have one)
3. Start the application with `java -jar target/datac-0.0.1-SNAPSHOT.jar`
4. Open [http://localhost:8080/] and login with the user `System` and password `Sys123`

## Current project status

Datac is still in a very early development status and most of the planned features don't exist yet. The text above
describes the vision of the tool that I want to build. If you find any bugs or have awesome ideas, feel free to create
issues for them!

## Disclaimer

This is currently a very early version which has nearly none of the final features.

Icons are taken from [http://www.iconsdb.com/icon-sets/web-2-blue-icons/].
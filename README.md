# Introduction 
This is the softMC Web Server, designed to provide APIs and HTTP server for Web applications running in the Servotronix softMC controller.

# Getting Started
1. You should set up your development environment (i.e: VSCode or Netbeans) to work with JDK1.8.0.
2. You should have Maven installed

# Build
1.	Make sure to update MCDEFS.JAVA with the new version code (in VER).
2.	Make sure to update POM.XML with the new version code (in <version> tag).
3.	Run maven clean and maven install commands.
4.	When build is done, see /target/MCWebServer-x.x.x-jar-with-dependencies.JAR

# RPM File
1.	Copy and rename the final JAR file into production as TPWebServer.jar
2.	Execute genipkg_java_files.sh [302/703] [server_version_num] (i.e: bash genipkg_java_files.sh 302 3.6.0).
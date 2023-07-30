# diktya1-consoleApp

![](https://img.shields.io/badge/Java-v11+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)

Console app variation for computer networks 1 assignment. Just the bare essentials.

## Features
- Automatically obtain the session codes from "ithaki" server
- Perform the required assignment tests
- Export packet statistics into convenient CSV format for further processing

## Installation
This project requires [maven](https://maven.apache.org/) to build and requires minimum Java version 11 and above. Developed using [Eclipse IDE](https://www.eclipse.org/downloads/).Tested on Java 17+. To build, just clone this repository and run:
`mvn clean verify` on your terminal or, if you have the maven plugin on eclipse installed, create a maven run configuration with `clean verify` as goal.

## Note
Please note that occasionally server may respond with invalid data. In that case, just restarting of program is enough.

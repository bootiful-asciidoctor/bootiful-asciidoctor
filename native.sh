#!/usr/bin/env bash
HERE="$(dirname $0)"
mvn  -DskipTests -f "$HERE/pom.xml"  spring-javaformat:apply clean  install
mvn -DskipTests -f $HERE/app/pom.xml -Pnative native:compile
$HERE/app/target/app

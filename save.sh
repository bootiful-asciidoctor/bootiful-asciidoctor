#!/usr/bin/env bash

export D=`date` 
mvn -DskipTests=true spring-javaformat:apply clean install && git commit -am "polish @ ${D} " && git push

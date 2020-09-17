#!/usr/bin/env bash

export D=`date`
mvn spring-javaformat:apply clean install && git commit -am "polish @ ${D} " && git push

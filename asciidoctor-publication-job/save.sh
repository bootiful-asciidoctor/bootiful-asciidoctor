#!/usr/bin/env bash

export GIT_PASSWORD=$GITHUB_PERSONAL_ACCESS_TOKEN
export GIT_REPOSITORY_URI=https://github.com/bootiful-asciidoctor/sample-book.git 
export GIT_USERNAME=joshlong

export D=`date`
mvn -DskipTests=true spring-javaformat:apply clean install && git commit -am "polish @ ${D} " && git push

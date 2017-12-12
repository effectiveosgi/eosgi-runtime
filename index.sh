#!/usr/bin/env bash
mvn -q -DskipTests -am -pl _index package && echo Indexed

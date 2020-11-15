#!/bin/bash

mvn deploy --settings .mvn/settings.xml -Dgpg.skip=false -DskipTests=true -B
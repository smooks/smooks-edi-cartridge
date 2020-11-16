#!/bin/bash

mvn deploy -T 1C --settings .mvn/settings.xml -Dgpg.skip=false -DskipTests=true -B
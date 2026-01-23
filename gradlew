#!/usr/bin/env sh
#
# Copyright 2017 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Use Java from path, if JAVA_HOME is not set
if [ -z "$JAVA_HOME" ]; then
    JAVA_EXE=$(command -v java)
else
    JAVA_EXE="$JAVA_HOME/bin/java"
fi

# Determine APP_HOME
APP_HOME=$(dirname "$(readlink -f "$0")")

# Determine GRADLE_HOME
if [ -z "$GRADLE_HOME" ]; then
    GRADLE_HOME=$(cd "$APP_HOME"; pwd)
fi

# Determine GRADLE_USER_HOME
if [ -z "$GRADLE_USER_HOME" ]; then
    GRADLE_USER_HOME="$HOME/.gradle"
fi

# Determine the location of the Gradle distribution.
# This is normally done by locating the "gradle-wrapper.jar" file in the "lib"
# subdirectory of the distribution.
GRADLE_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$GRADLE_JAR" ]; then
    echo "Error: Gradle wrapper JAR not found."
    echo "Please ensure '$GRADLE_JAR' exists."
    exit 1
fi

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
JVM_OPTS="$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS"

exec "$JAVA_EXE" $JVM_OPTS -classpath "$GRADLE_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
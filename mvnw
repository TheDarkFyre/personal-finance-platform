#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up Shell Script
# ----------------------------------------------------------------------------

set -e

MAVEN_VERSION="3.9.6"
MAVEN_DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
WRAPPER_DIR="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_HOME="${WRAPPER_DIR}/apache-maven-${MAVEN_VERSION}"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Maven not found in ~/.m2. Downloading Apache Maven ${MAVEN_VERSION}..."
    mkdir -p "$WRAPPER_DIR"
    TMP_TAR="/tmp/maven-${MAVEN_VERSION}.tar.gz"
    if command -v curl >/dev/null 2>&1; then
        curl -sSL "$MAVEN_DIST_URL" -o "$TMP_TAR"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$MAVEN_DIST_URL" -O "$TMP_TAR"
    else
        echo "Error: curl or wget is required to download Maven wrapper." >&2
        exit 1
    fi
    tar -xzf "$TMP_TAR" -C "$WRAPPER_DIR"
    rm -f "$TMP_TAR"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"

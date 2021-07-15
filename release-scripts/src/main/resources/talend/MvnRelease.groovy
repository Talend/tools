package org.talend.tools.release

//
// CONFIG:
//
// args[0]: root pom location
// args[1]: maven location if not available in the path
//
// ex: groovy /location/of/my/pom.xml /location/of/maven/bin/mvn
//

import static java.util.Locale.ROOT

/**
 *  Copyright (C) 2018 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

def pom
if (args.length > 0) {
    pom = new File(args[0])
} else {
    pom = new File('pom.xml')
}
if (!pom.exists()) {
    throw new IllegalArgumentException("'${pom.absolutePath}' doesn't exists")
}
def project = new XmlParser().parseText(pom.text)
def artifactId = project.artifactId.text()
if (artifactId == null) {
    throw new IllegalArgumentException("No artifactId in the pom ${pom.absolutePath}")
}
if (artifactId.endsWith('-parent')) {
    artifactId = artifactId.substring(0, artifactId.length() - '-parent'.length())
}
def version = project.version.text()
if (version == null) {
    throw new IllegalArgumentException("${pom.absolutePath} doesn't define a version")
}
if (!version.endsWith('-SNAPSHOT')) {
    throw new IllegalArgumentException("${version} is not a snapshot, cancelling")
}
def release = version.substring(0, version.length() - '-SNAPSHOT'.length())
def versions = release.split('\\.') as List<String>
if (versions.size() != 3) {
    throw new IllegalArgumentException("Invalid version, it should use x.y.z-SNAPSHOT syntax")
}

// find maven
def path = System.getenv('PATH')
if (path == null) {
    path = System.getenv('Path')
}
def mvn = args.length > 1 ? args[1] : (path.split(File.pathSeparator) as List<String>).findAll {
    new File(it, 'mvn').exists()
}.collect {
    new File(it, "mvn${System.getProperty('os.name').toLowerCase(ROOT).contains('win') ? '.cmd' : ''}")
}.first().absolutePath

// prepare versions and case handling
def isMaintenance = versions.get(2) != '0'
def tagName = "${artifactId}-${release}"
def nextSnapshot = !isMaintenance ?
        "${versions.subList(0, versions.size() - 2).join('.')}.${Integer.parseInt(versions.get(1)) + 1}.0-SNAPSHOT" :
        "${versions.subList(0, versions.size() - 1).join('.')}.${Integer.parseInt(versions.get(2)) + 1}-SNAPSHOT"
// only for !isMaintenance case
def branchVersion = "${versions.subList(0, versions.size() - 1).join('.')}.${Integer.parseInt(versions.last()) + 1}-SNAPSHOT"
def branchName = "maintenance/${artifactId}-${versions.subList(0, versions.size() - 1).join('.')}"

println("Will launch the '${release}' release in tag ${tagName} and use " +
        "'${nextSnapshot}' for next development iteration" +
        (isMaintenance ? '' : " and '${branchVersion}' for the maintenance branch which is named '${branchName}'"))

// launch mvn release:prepare
def prepareCommand = [mvn, 'release:prepare', '--batch-mode',
                      "-Dtag=${tagName}", "-DpushChanges=true",
                      "-DreleaseVersion=${release}", "-DdevelopmentVersion=${nextSnapshot}"]
println(prepareCommand)
def prepare = new ProcessBuilder(prepareCommand as String[])
        .inheritIO()
        .directory(pom.parentFile)
        .start()
        .waitFor()
if (prepare != 0) {
    throw new IllegalArgumentException("Invalid prepare exit code: ${prepare}")
}

// launch mvn release:perform
def performCommand = [mvn, 'release:perform', '--batch-mode']
println(performCommand)
def perform = new ProcessBuilder(performCommand as String[])
        .inheritIO()
        .directory(pom.parentFile)
        .start()
        .waitFor()
if (perform != 0) {
    throw new IllegalArgumentException("Invalid perform exit code: ${perform}")
}

if (!isMaintenance) {// create a branch
    def branchCommand = [mvn, 'release:branch', '--batch-mode',
                         '-DupdateBranchVersions=true', '-DupdateWorkingCopyVersions=false',
                         "-DbranchName=${branchName}",
                         "-DreleaseVersion=${release}", "-DdevelopmentVersion=${branchVersion}"]
    println(branchCommand)
    def branch = new ProcessBuilder(branchCommand as String[])
            .inheritIO()
            .directory(pom.parentFile)
            .start()
            .waitFor()
    if (branch != 0) {
        throw new IllegalArgumentException("Invalid branch exit code: ${branch}")
    }
}

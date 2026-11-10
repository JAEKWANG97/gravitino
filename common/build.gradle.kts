/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
  `maven-publish`
  `java-test-fixtures`
  id("java")
  id("idea")
}

dependencies {
  implementation(project(":api"))

  implementation(libs.commons.collections4)
  implementation(libs.commons.io)
  implementation(libs.commons.lang3)
  implementation(libs.guava)
  implementation(libs.jackson.annotations)
  implementation(libs.jackson.datatype.jdk8)
  implementation(libs.jackson.datatype.jsr310)
  implementation(libs.jackson.databind)
  implementation(libs.jakarta.validation.api)
  implementation(libs.slf4j.api)

  annotationProcessor(libs.lombok)
  compileOnly(libs.commons.dbcp2)
  compileOnly(libs.jackson.dataformat.yaml)
  compileOnly(libs.lombok)
  testAnnotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)

  testImplementation(libs.jackson.dataformat.yaml)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)

  testRuntimeOnly(libs.junit.jupiter.engine)

  testFixturesApi(project(":api"))
  testFixturesApi(libs.junit.jupiter.api)
  testFixturesImplementation(libs.commons.lang3)
}

fun getGitCommitId(gitRoot: File = rootDir): String {
  return try {
    val output = ByteArrayOutputStream()
    project.exec {
      workingDir(gitRoot)
      commandLine("git", "--git-dir", File(gitRoot, ".git").absolutePath, "rev-parse", "--verify", "HEAD")
      standardOutput = output
      errorOutput = ByteArrayOutputStream()
    }
    output.toString(Charsets.UTF_8.name()).trim()
  } catch (e: Exception) {
    logger.warn("Unable to get Git commit id: ${e.message}")
    ""
  }
}

extra["getGitCommitId"] = ::getGitCommitId

val propertiesFile = "src/main/resources/gravitino-build-info.properties"
fun writeProjectPropertiesFile() {
  val propertiesFile = file(propertiesFile)
  if (propertiesFile.exists()) {
    propertiesFile.delete()
  }

  val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

  val compileDate = dateFormat.format(Date())
  val projectVersion = project.version.toString()
  val commitId = getGitCommitId()

  propertiesFile.parentFile.mkdirs()
  propertiesFile.createNewFile()
  propertiesFile.writer().use { writer ->
    writer.write(
      "#\n" +
        "# Licensed to the Apache Software Foundation (ASF) under one\n" +
        "# or more contributor license agreements.  See the NOTICE file\n" +
        "# distributed with this work for additional information\n" +
        "# regarding copyright ownership.  The ASF licenses this file\n" +
        "# to you under the Apache License, Version 2.0 (the\n" +
        "# \"License\"); you may not use this file except in compliance\n" +
        "# with the License.  You may obtain a copy of the License at\n" +
        "#\n" +
        "#  http://www.apache.org/licenses/LICENSE-2.0\n" +
        "#\n" +
        "# Unless required by applicable law or agreed to in writing,\n" +
        "# software distributed under the License is distributed on an\n" +
        "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
        "# KIND, either express or implied.  See the License for the\n" +
        "# specific language governing permissions and limitations\n" +
        "# under the License.\n" +
        "#\n"
    )
    writer.write("project.version=$projectVersion\n")
    writer.write("compile.date=$compileDate\n")
    writer.write("git.commit.id=$commitId\n")
  }
}

tasks {
  jar {
    doFirst() {
      writeProjectPropertiesFile()
      if (!file(propertiesFile).exists()) {
        throw GradleException("Failed to generate $propertiesFile.")
      }
    }

    from("src/main/resources") {
      include("gravitino-build-info.properties").duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
  }

  clean {
    delete("$propertiesFile")
  }
}

apply(from = rootProject.file("dev/test-git-commit-id.gradle"))

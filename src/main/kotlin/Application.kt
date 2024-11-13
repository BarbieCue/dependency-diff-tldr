/*
* Copyright Careem, an Uber Technologies Inc. company
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File

fun main(args: Array<String>) {

  val parser = ArgParser("gradle-dependency-diff")

  val old by parser.option(ArgType.String, "old", "o", "Path to the old dependency list").default("")
  val new by parser.option(ArgType.String, "new", "n", "Path to the new dependency list").default("")

  val outputFormat by parser.option(ArgType.String, fullName = "output-format", shortName = "f", description = "Output type, \"plain\" and \"json\" are supported").default("plain")
  val sideEffects by parser.option(ArgType.Boolean, fullName = "side-effects", shortName = "s", description = "Print out any side effects of upgrading the dependencies").default(false)
  val collapse by parser.option(ArgType.String, fullName = "collapse", shortName = "c", description = "Collapse packages of the same group and version. Example: \"org.example.math\"").default("")

  parser.parse(args)

  val oldContents = File(old).readText()
  val newContents = File(new).readText()
  val collapsePackages = collapse.split(" ")

  print(tldr(oldContents, newContents).toString(collapsePackages, outputType = outputFormat.toOutputType()))

  if (sideEffects) {
    val upgradeEffects = upgradeEffects(oldContents, newContents, collapsePackages)
    if (upgradeEffects.isNotEmpty()) {
      println()
      println("Upgrade Side Effects")
      print(upgradeEffects)
    }
  }
}

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

fun tldr(old: String, new: String): VersionDifferences {
  return dependencyDifferences(old, new)
}

fun dependencyDifferences(old: String, new: String): VersionDifferences {
  val oldDependencies = extractDependencies(old)
  val newDependencies = extractDependencies(new)

  val addedDependencies = newDependencies - oldDependencies
  val removedDependencies = oldDependencies - newDependencies
  return partitionDifferences(removedDependencies, addedDependencies)
}

private fun extractDependencies(deps: String): Set<VersionedDependency> {
  return deps.split('\n')
    .asSequence()
    .dropWhile { !it.startsWith("+--- ") }
    .takeWhile { it.isNotEmpty() }
    .map {
      val artifactStart = it.indexOf("--- ")
      it.substring(artifactStart + 4)
    }
    // ignore added or removed local project modules
    .filter { !it.startsWith("project ") }
    .map { artifactBase ->
      val noVersion = artifactBase.indexOf(':') == artifactBase.lastIndexOf(':')
      val artifact = when { // org.jetbrains.kotlinx:kotlinx-coroutines-android
        noVersion -> {
          artifactBase.substringBefore(' ')
        }
        else -> {
          artifactBase.substringBeforeLast(':')
        }
      }
      val versionInfo = artifactBase.substringAfterLast(':') // 1.6.0 (c)
      val canonicalVersionInfo = when { // 1.6.0
        "->" in versionInfo -> versionInfo.substringAfter("-> ").substringBefore(' ')
        "(*)" in versionInfo || "(c)" in versionInfo -> versionInfo.substringBefore(" (")
        else -> versionInfo
      }
      VersionedDependency(artifact, canonicalVersionInfo)
    }
    .toSet()
}

data class VersionedDependency(
  val artifact: String, // org.jetbrains.kotlinx:kotlinx-coroutines-android
  val version: String,  // 1.6.0
  val alternativeVersion: String? = null // 1.5.0
) {
  val group by lazy { artifact.substringBefore(':').trim() } // org.jetbrains.kotlinx
}

data class VersionDifferences(
  val additions: List<VersionedDependency>,
  val removals: List<VersionedDependency>,
  val upgrades: List<VersionedDependency>
)

private fun partitionDifferences(
  removed: Set<VersionedDependency>,
  added: Set<VersionedDependency>
): VersionDifferences {
  val additions = mutableListOf<VersionedDependency>()
  val upgrades = mutableListOf<VersionedDependency>()
  val removals = mutableListOf<VersionedDependency>()

  val mutableRemovedMap = removed.associateTo(mutableMapOf()) { it.artifact to it.version }
  for ((artifact, version) in added) {
    val removedVersion = mutableRemovedMap[artifact]
    when {
      removedVersion != null -> {
        mutableRemovedMap.remove(artifact)
        upgrades.add(VersionedDependency(artifact, version, alternativeVersion = removedVersion))
      }
      else -> {
        additions.add(VersionedDependency(artifact, version))
      }
    }
  }

  for ((artifact, version) in mutableRemovedMap) {
    removals.add(VersionedDependency(artifact, version))
  }

  return VersionDifferences(
    additions.sortedBy { it.artifact },
    removals.sortedBy { it.artifact },
    upgrades.sortedBy { it.artifact }
  )
}



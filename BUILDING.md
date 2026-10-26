# Building the 1.7.10 backend

Use the companion MTR fork's [build guide](https://github.com/emil2001/Minecraft-Transit-Railway/blob/gtnh-1.7.10/BUILDING.md). It covers JDK 21, the Gradle 8.14 wrappers, the required sibling checkout names `mtr` and `mappings`, and the external inputs.

Pass absolute paths with `mtrLog4j`, `mtrDependencies` and `mtrAngelica`. The backend's nested Core preparation receives the same properties. From this repository's root, the backend test command is:

```sh
./gradlew -p forge/1.7.10-mapping test \
  "-PmtrLog4j=/absolute/path/to/gtnh-libraries" \
  "-PmtrDependencies=/absolute/path/to/port-dependencies" \
  "-PmtrAngelica=/absolute/path/to/angelica-2.2.28.jar"
```

The complete production mod is built from the companion repository with `./gradlew -p gtnh test productionBuild` and those same properties, as shown in the shared guide. The dependency directory must contain the official Minecraft 1.20.4 client input, the pinned occlusion-culling snapshot and the five JARs in [`narrator-runtime.json`](forge/1.7.10-mapping/build-inputs/narrator-runtime.json).

Fresh external retrieval of all pinned inputs remains untested. The exact occlusion-culling snapshot was recovered from an upstream cache; its current online availability has not been verified. A local Maven cache can supplement the configured online repositories, but this publication does not claim a fresh offline build or provide a port binary release.

# Minecraft Mappings for the GTNH port

The `gtnh-1.7.10` branch adds the Minecraft 1.7.10 Forge compatibility layer used by [Minecraft Transit Railway 4.0.5 for GTNH](https://github.com/emil2001/Minecraft-Transit-Railway/tree/gtnh-1.7.10). The target pack is GT New Horizons 2.9.0-RC-2.

This fork builds on [Minecraft Mappings](https://github.com/Minecraft-Transit-Railway/Minecraft-Mappings), created by Jonathan Ho and its contributors. The [original README](README.upstream.md) explains the upstream mapping API and generation process.

## Building

The 1.7.10 backend is in `forge/1.7.10-mapping` and uses Java 21 and Gradle 8.14. [BUILDING.md](BUILDING.md) links the shared checkout layout, external input requirements and build commands. Retrieval of every dependency in a fresh environment remains untested.

## License and credits

Minecraft Mappings is licensed under the [MIT License](LICENSE), with the original copyright notice for Jonathan Ho (2023). Preserve the upstream authorship, source notices and dependency licenses when redistributing this fork. The companion [third-party notes](https://github.com/emil2001/Minecraft-Transit-Railway/blob/gtnh-1.7.10/THIRD_PARTY.md) record the unresolved attribution issues from the port review.

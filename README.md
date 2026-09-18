# FuzziIds

[![Build status](https://github.com/MrFuzzihead/FuzziIds/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/MrFuzzihead/FuzziIds/actions/workflows/build-and-test.yml)
[![Latest release](https://img.shields.io/github/v/release/MrFuzzihead/FuzziIds?include_prereleases&sort=semver)](https://github.com/MrFuzzihead/FuzziIds/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

Logs all used, available, and conflicting registry IDs in a Minecraft 1.7.10 modpack to CSV
files. EndlessIDs-aware: capacities are read from the *live* registries at dump time, so the
reports automatically reflect whatever ID-extending mod is installed (EndlessIDs,
NotEnoughIDs, ...) and its per-module configuration — or vanilla limits if none is present.

## Tracked categories

| Category      | Captured via (early mixin)                        | Capacity source                                                                 |
|---------------|---------------------------------------------------|---------------------------------------------------------------------------------|
| Biomes        | `BiomeGenBase(int, boolean)` ctor                 | `BiomeGenBase.getBiomeGenArray().length` (256 vanilla / 65,536 with EndlessIDs) |
| Potions       | `Potion(int, boolean, int)` ctor                  | `Potion.potionTypes.length` (32 / 65,536)                                       |
| Enchantments  | `Enchantment(int, int, EnumEnchantmentType)` ctor | `Enchantment.enchantmentsList.length` (256 / 32,768)                            |
| Entities      | `EntityList.addMapping`                           | `ExtendedConstants.maxEntityID + 1` when EndlessIDs is loaded, else 256         |
| Data watchers | `DataWatcher.addObjectByDataType`                 | `ExtendedConstants.maxWatchableID + 1` when EndlessIDs is loaded, else 32       |
| Dimensions    | `DimensionManager.registerDimension`              | unbounded                                                                       |
| Providers     | `DimensionManager.registerProviderType`           | unbounded                                                                       |

The early mixins capture *registration events* (including conflicts where one registration
overwrites, rejects, or would crash against an existing one), while the dump-time pass scans
the live registries for the authoritative used list. Captured registrations that are missing
from the final registry are reported in `conflicts.csv` as rejected/overwritten.

## Reports

Written to `config/FuzziIds/` (configurable):

- `<category>.csv` — used IDs: `id,name,owner_mod,java_class`
- `<category>_available.csv` — free ID ranges: `start_id,end_id`
- `datawatchers.csv` — per entity class: `owner_class,watcher_id,data_type,declaring_class,owner_mod`
- `dimensions.csv` / `providers.csv` — dimension and provider-type registrations
- `conflicts.csv` — `category,id,existing,attempted,owner_mod`: one row per *collision*. `existing`
  describes the entry already present at that ID (including, where known, the mod that holds it),
  `attempted` describes the incoming registration that collided with it, and `owner_mod` is the
  owner of the **attempted** registration. Rejected registrations (captured but absent from the
  final registry) and duplicate-dimension crashes are reported here too. Dimension un/re-register
  cycles around world loads are tracked (via `unregisterDimension`), and a mod re-registering its
  own provider type is not treated as a conflict.
- `limits.csv` — `category,capacity,used,free,limit_source`

`owner_mod` is attributed via the active FML mod container at registration time. Vanilla
entries registered during bootstrap show `minecraft`; `unattributed` means the registration
could not be attributed (e.g. lazily class-initialized registries, or registration through
unusual code paths).

## Triggers

- FML post-initialization (after all mods registered)
- Every world load (catches late-registered dimensions; toggleable in the config)
- `/fuzziids dump` (permission level 2) re-writes all reports on demand

## Building

Standard GTNH toolchain: `./gradlew build`. Run a dev server with `./gradlew runServer`.

# Contributing

Thanks for helping with xAuctions.

## How to contribute

1. Fork the repository and create a branch from `main`.
2. Make a focused change. Match existing package layout and naming.
3. Build and smoke-test your change before opening a pull request.
4. Open a pull request. Describe the problem and the fix.

By submitting a contribution, you license it under the Apache License 2.0 unless you state otherwise.

## Building

Requires **JDK 21**.

```bash
mvn clean package
```

The shaded jar is `target/xAuctions-<version>.jar`.

Optional obfuscation (not used for public releases):

```bash
mvn clean install -Pobfuscate
```

## Compatibility (required for releases)

Every public release must support **Paper, Folia, Purpur, Spigot, and Bukkit** tags for **Minecraft 1.20.1 through 26.3**.

- Keep `api-version: '1.20'` and `folia-supported: true` in `plugin.yml`.
- Compile against Paper API 1.20.4 (see `pom.xml`). Do not call APIs that only exist on newer Minecraft unless you feature-detect.
- Use `PlatformAdapter` (Folia region schedulers). Do not use `BukkitScheduler` for gameplay work.
- Publish tags must list every loader and every version in `release/supported-minecraft.json`.

## Issues

Use GitHub issues for bugs and feature requests. Do not file public issues for security problems; see [SECURITY.md](.github/SECURITY.md).

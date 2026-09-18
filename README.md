# xAuctions

Scalable auction house plugin for Paper, Folia, Purpur, Spigot, and Bukkit servers.

## Features

- Buy-it-now auction marketplace with searchable GUI
- Multi-currency economy hooks (Vault, PlayerPoints, DonutCore)
- JSON and SQL storage backends with HikariCP pooling
- Folia-compatible scheduling via `PlatformAdapter`
- Listing limits, fees, taxes, blacklists, and anti-dupe protections
- PlaceholderAPI and Discord webhook integrations
- Public developer API (`AuctionService`, events, models)

## Requirements

- Java 21
- Minecraft **1.20.1 through 26.3**
- Paper, Folia, Purpur, Spigot, or Bukkit (Paper/Folia recommended)

See [`release/supported-minecraft.json`](release/supported-minecraft.json) for the full publish matrix.

## Installation

1. Download the latest release from [Modrinth](https://modrinth.com/plugin/xauctions-advanced-auctions-house) or [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/xauctions).
2. Place the jar in your server `plugins/` folder.
3. Restart the server and configure `plugins/xAuctions/config.yml`.

## Building

```bash
mvn clean package
```

Output: `target/xAuctions-1.3.0.jar`

## Usage

- `/ah` — open the auction house
- `/ah sell <price>` — list the item in hand
- `/sell <price>` — optional top-level sell command (disabled by default)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](.github/SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).

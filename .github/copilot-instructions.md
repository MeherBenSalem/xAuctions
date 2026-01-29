# xAuctions - AI Coding Agent Guide

## Project Overview
xAuctions is a premium Spigot/Paper auction house plugin for Minecraft servers (Java 17, Paper API 1.20.4). It's a modular, scalable system designed for SMP and network environments with multi-storage backend support.

## Architecture

### Core Components
- **XAuctionsPlugin**: Main entry point with strict initialization order (see `initializeManagers()` lines 136-180)
  1. MessageManager → EconomyManager → StorageProvider → GuiManager → AuctionManager → AntiDupeManager → TaxManager → ListingLimitManager → Blacklist Managers
- **Service Layer**: `api/service/` contains public interfaces (`AuctionService`, `EconomyService`)
  - Implementations in `internal/service/impl/` - use service layer for business logic, not managers directly
- **Storage Abstraction**: `StorageProvider` interface with JSON and SQL implementations
  - All storage operations return `CompletableFuture<T>` for async execution
  - SQL uses HikariCP connection pooling (config in `SqlStorageProvider.java:30-48`)

### Package Structure
```
api/           - Public API contracts (events, services, models, storage interface)
core/          - Core implementation (commands, listeners, managers)
config/        - Configuration handling (PluginConfig, menu configs)
gui/           - Menu system with framework/ for base classes
storage/       - Storage implementations (json/, sql/)
economy/       - Economy provider implementations (Vault, PlayerPoints)
security/      - AntiDupeManager for preventing duplication exploits
internal/      - Internal service implementations (not for external use)
```

## Critical Patterns

### Manager Initialization Order
**ALWAYS** follow the dependency order in `XAuctionsPlugin.initializeManagers()`. Adding a new manager? Insert it at the correct dependency level - never break the chain. Managers register themselves as listeners in constructors (see `GuiManager`, `AntiDupeManager`).

### GUI Framework
- Extend `BaseMenu` for new menus (NOT `AbstractMenu` directly)
- Use `MenuItem` with `MenuAction` for click handlers
- Navigation: `plugin.getGuiManager().openMenu(player, new SomeMenu(plugin))`
- Example: [gui/menus/AuctionsMenu.java](gui/menus/AuctionsMenu.java#L142-L155)
- Menus can be YAML-driven - see `ConfigurableMenu` and [src/main/resources/menus/](src/main/resources/menus/)

### Async Operations
All storage operations are async using `CompletableFuture`:
```java
storageProvider.saveAuction(auction).thenAccept(v -> {
    // Run on async thread - schedule sync tasks with Bukkit.getScheduler()
});
```

### Model Classes
Models use **manual** getters/setters (NO Lombok) with builder pattern:
```java
Auction auction = Auction.builder()
    .auctionId(UUID.randomUUID())
    .sellerUuid(player.getUniqueId())
    .build();
```
See [api/model/Auction.java](api/model/Auction.java#L116-L203) for builder implementation.

### Result Pattern
Use `api/model/Result<T>` for operation outcomes - never throw exceptions from service layer:
```java
CompletableFuture<Result<Auction>> result = auctionService.createAuction(request);
// Result contains .isSuccess(), .getData(), .getError()
```

### Configuration Access
- Plugin config: `plugin.getPluginConfig()` → `PluginConfig` instance
- Auction settings: `plugin.getPluginConfig().getAuctionSettings()` → `AuctionSettings` record
- Never read YAML directly - always through `PluginConfig`

## Build & Development

### Maven Commands
```bash
# Build (includes obfuscation via yGuard)
apache-maven-3.9.6/bin/mvn clean install

# Build without obfuscation (faster for dev)
apache-maven-3.9.6/bin/mvn clean package

# Output: target/xAuctions-1.0.0.jar (or -obf.jar after install)
```

### Shaded Dependencies
HikariCP and Jedis are relocated to `io.nightbeam.studio.xauctions.libs.*` (see [pom.xml](pom.xml#L187-L198)). Don't import original packages in new code.

### Obfuscation Exclusions
Keep classes NOT obfuscated ([pom.xml](pom.xml#L217-L253)):
- Main plugin class
- Commands, listeners, integrations (reflection)
- Config, model, storage classes (field names map to YAML/DB)
- Shaded libraries

## Integration Points

### External Dependencies
- **Vault**: Economy provider (soft-depend)
- **PlaceholderAPI**: Expansion in `addons/xAuctionsExpansion` (soft-depend)
- **PlayerPoints**: Alternative economy (soft-depend)
- **Discord Webhook**: Optional notifications via `addons/DiscordWebhook`

### Public API
External plugins access via `XAuctionsAPI.getInstance().getAuctionService()` - see [api/XAuctionsAPI.java](api/XAuctionsAPI.java)

## Security & Anti-Exploit

### AntiDupeManager
Locks players during item transactions to prevent duplication. Always:
1. Lock player before item operations
2. Perform async storage
3. Unlock in both success AND failure paths
Permission bypass: `xauctions.bypass.antidupe`

### Creative Mode Protection
Check `plugin.getPluginConfig().getAuctionSettings().allowCreative()` before accepting listings - default is `false` to prevent economy exploits.

## Testing & Debugging
- Enable debug mode: `config.yml` → `general.debug: true`
- Debug logs: `plugin.debug("message")` (only logs if debug enabled)
- Check initialization in console - each manager logs when initialized
- Common issues: Manager initialization order violated, async operations on main thread

## Conventions
- **Naming**: Use full words, no abbreviations (e.g., `AuctionManager` not `AucMgr`)
- **Permissions**: Format `xauctions.<category>.<action>` (see [plugin.yml](src/main/resources/plugin.yml#L36-L88))
- **Messages**: All user-facing text in `messages.yml` via `MessageManager`
- **Events**: Fire custom events (`AuctionCreateEvent`, `AuctionBuyEvent`) before operations - check `isCancelled()`

## Common Workflows

### Adding a New Menu
1. Create class extending `BaseMenu` in `gui/menus/`
2. Override `buildMenu()` to populate items using `setItem(slot, item, action)`
3. Register navigation in parent menu: `plugin.getGuiManager().openMenu(player, new YourMenu(plugin))`

### Adding a Storage Field
1. Add field to `Auction` model with getter/setter
2. Update `AuctionBuilder` with field and method
3. Modify `ItemSerializer.serializeAuction()` for JSON storage
4. Update SQL schema in `SqlStorageProvider.createTable()` and CRUD queries

### Adding a Manager
1. Create in `core/managers/`
2. Add field + getter in `XAuctionsPlugin`
3. Initialize in `initializeManagers()` at correct dependency level
4. If needs events, implement `Listener` and register in constructor OR in `registerListeners()`

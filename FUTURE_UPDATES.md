# xAuctions — Architecture Analysis & Future Updates Roadmap

> Generated: February 23, 2026  
> Version analyzed: 1.1.0  
> Target: Production servers (100–300 players), Paper + Folia

---

## Table of Contents

1. [Architecture Review](#section-1--architecture-review)
2. [Feature Expansion Suggestions](#section-2--feature-expansion-suggestions)
3. [Competitive Improvements](#section-3--competitive-improvements)
4. [Optimization Suggestions](#section-4--optimization-suggestions)
5. [Code Modernization](#section-5--code-modernization)
6. [Security Review](#section-6--security-review)
7. [Prioritized Roadmap](#prioritized-roadmap)

---

## SECTION 1 — Architecture Review

### Architectural Weaknesses

**1. `AuctionManager` is a pure delegation shell.**  
`AuctionManager` contains zero business logic — every method is a one-liner that calls `plugin.getAuctionService()`. It exists only to maintain a "manager" naming convention while `AuctionServiceImpl` does all the real work. This creates confusion: external callers reach the service through two different entry points (`plugin.getAuctionManager()` and `plugin.getAuctionService()`), both of which do the same thing. One of them must be canonicalized and the other removed.

**2. `XAuctionsPlugin` is a God-Object.**  
The main class holds 13+ fields, initializes all subsystems, wires all dependencies, registers listeners, registers commands, and loads integrations — all in one file. There is no service locator, DI container, or module graph. Every class reaches back to the plugin singleton via `plugin.getX()`, creating a tight web of hidden coupling that makes unit testing impossible and refactoring hazardous.

**3. `SqlStorageProvider.init()` hardcodes credentials.**  
Lines 33–35 of `SqlStorageProvider` set `jdbc:mysql://localhost:3306/minecraft`, `root`, and `password` directly in code with a `// TODO: Load from config.yml properly` comment. On a real production server, starting the plugin with SQL storage silently connects to a localhost database with root credentials. This is a critical defect — an unconfigured SQL provider will either silently fail or write to the wrong server.

**4. `NetworkSyncManager` is declared but never initialized.**  
`NetworkSyncManager` and `RedisManager` exist in the codebase but are never instantiated in `initializeManagers()`. Redis capability is dead code. The `invalidateCache` method on `SqlStorageProvider` is a no-op. On multi-server environments, two servers can sell the same item between the atomic DB lock and the local GUI state refresh, because no invalidation signal is ever broadcast.

**5. `AntiDupeManager` is effectively non-functional.**  
The `scanInventory` and `isAllowed` methods are stubs with TODO comments. There is no NBT-based item tagging, no in-flight lock set, and no inventory scanning. The only actual protection is the SQL `attemptBuy` atomic update — which only guards SQL backends. The JSON backend's `synchronized (auctionCache)` block is correct but relies entirely on in-memory state; a server restart between listing and purchase loses all lock guarantees.

**6. `PlayerBlacklistManager` persists to `config.yml`.**  
`blacklist()` and `unblacklist()` call `plugin.getConfig().set(...)` and `plugin.saveConfig()`. This writes to `config.yml`, interleaving runtime-mutated data with static server configuration. If the server reloads the plugin, the blacklist is re-read from the same file without issue, but it means every operator-configured value in `config.yml` is now at risk of being overwritten by plugin state. Blacklists must live in a dedicated `blacklist.yml` or in the database.

**7. Dual `GuiManager` open-menu tracking uses a non-concurrent `HashMap`.**  
`openMenus` in `GuiManager` is a plain `HashMap<UUID, AbstractMenu>`. GUI events fire on the main thread so this is technically safe on standard Paper. However, it becomes unsafe under Folia where inventory events can fire on region threads. This must be a `ConcurrentHashMap`.

**8. `PluginConfig` is bypassed by `TaxManager` and `ItemBlacklistManager`.**  
Both call `plugin.getConfig()` directly instead of going through `PluginConfig`. This bypasses the centralized configuration layer. If validated configuration handling is added to `PluginConfig`, these two managers will silently ignore it.

**9. `AuctionServiceImpl.getActiveCount()` has a race condition.**  
The `countInitialized` flag is a plain `boolean`. Between the check `if (!countInitialized)` and the assignment `countInitialized = true`, two concurrent callers on different threads can both enter the body, fire two `loadActiveAuctions()` futures, and set `activeCount` twice in an undefined order. Must use `AtomicBoolean` and `compareAndSet`.

**10. Version check blocks the main thread during `onEnable`.**  
`fetchRemoteVersion()` is called synchronously in the banner-print block (before `initializeManagers()`) on the main thread, opening an `HttpURLConnection` with up to 4 seconds of combined timeout. It is called a second time asynchronously in `checkForUpdates()`, making the first call completely redundant.

---

### Code Smells

- **`Result.getData()` vs `Result.getValue()`** — `AuctionManager` comments reference `.getData()` but the method is `getValue()`. Indicates a partial rename that was never completed.
- **Raw `§c` color codes in service layer** — `AuctionServiceImpl` and `PlayerConnectionListener` contain ~12 hardcoded message strings scattered outside `MessageManager`.
- **`PlayerSellingMenu` bypasses `BaseMenu` framework** — Extends `AbstractMenu` directly, manages a raw `HashMap<Integer, Auction>` instead of using `MenuItem` actions already provided by `BaseMenu.handleClick()`.
- **`AuctionHistoryMenu` and `PlayerSellingMenu` duplicate the same UI** — Both show a player's own auctions with collect/cancel actions.
- **`SchedulerUtils.adapter()` recreates `PlatformAdapter` on every call** — The fallback path calls `new PlatformAdapter(plugin)` which internally calls `Class.forName` on each invocation.
- **`InputListener` uses deprecated `AsyncPlayerChatEvent`** — Paper 1.19+ deprecates this in favour of `AsyncChatEvent`. Will be removed in a future API version.
- **`Auction.requestTime` is never set by the builder** — The builder has no `.requestTime()` method exposed, so `build()` always passes `requestTime = 0`.

---

### Performance Risks

- **`AuctionsMenu.getData()` calls `.join()` on the main thread** — `cachedAuctions = plugin.getStorageProvider().loadActiveAuctions().join()` runs synchronously on the GUI render path. With 5,000 auctions on SQL this can freeze the server for 200–500ms on every menu open. On Folia it will deadlock the region thread.
- **`JsonStorageProvider` rewrites the entire file on every mutation** — Every `saveAuction`, `updateAuction`, or `deleteAuction` call serializes the full auction list to disk. Under 100 concurrent buyers this produces massive I/O write storms.
- **`invalidateCache()` in JSON reloads the entire file** — Any Redis invalidation message triggers a full file read from disk, defeating the purpose of having an in-memory cache.
- **`AuctionHistoryMenu.update()` fires a new DB query on every render** — No debounce or cooldown. Rapid clicks fire multiple concurrent `loadPlayerAuctions` futures that all write to `cachedData` without synchronization.
- **`PlayerConnectionListener.onJoin` calls `eco.deposit()` on an async thread** — Vault's economy implementation is not guaranteed thread-safe. This is a silent data corruption risk.

---

### Thread Safety Issues

- **`InputListener.awaitingSearch` is a plain `HashSet`** accessed from both the `AsyncPlayerChatEvent` async thread and the `awaitSearch()` main-thread call. Concrete race condition.
- **`AuctionHistoryMenu.cachedData`** is written from async `thenAccept` and read from the main thread in `getData()` without `volatile` or synchronization. Data race.
- **`AuctionServiceImpl.activeCount` / `countInitialized`** — non-atomic check-then-act between two threads.
- **`AntiDupeManager` has no actual lock** — no transaction lock protecting item extraction from player inventories between the `precheck` stage and the `attemptBuy` stage.
- **`EconomyManager.providers` is a plain `HashMap`** — safe today because init is single-threaded, but fragile as the plugin grows.

---

### Scalability Issues (100+ Players)

- **No pagination on auction list queries** — With 5,000 active auctions, every `/ah` open fetches all of them. A `LIMIT/OFFSET` page approach is required.
- **No in-process auction cache for SQL backend** — `SqlStorageProvider.invalidateCache()` is a no-op. Every GUI page change re-queries the database. At 100 concurrent players browsing, this generates hundreds of `SELECT *` queries per minute.
- **No expiration scheduler** — Expired auctions are detected lazily inline. No background task sweeps and resolves them.
- **Discord webhook has no rate limiting or queue** — Under 100 simultaneous sales, 100 HTTP connections fire concurrently.
- **`RedisManager` subscription runs on a bare `new Thread(...)`** — Unmanaged thread crashes silently without restart.

---

## SECTION 2 — Feature Expansion Suggestions

### Small Features (10)

| # | Feature | What it does | Why | Systems | Monetization | Performance |
|---|---|---|---|---|---|---|
| 1 | **Sort & Filter Persistence** | Remembers last-used sort order and category filter per player | Premium plugins remember preferences | `PlayerPreferences` cache | None | Negligible |
| 2 | **Price History Indicator** | Adds lore showing `Recently sold for X–Y` based on last 5 sales of that type | Buyers make informed decisions | `PriceHistoryCache`, `item_type` DB column | Gate full history behind VIP | Low — TTL cache |
| 3 | **Favorite Items / Watchlist** | Right-click any auction to watch it; `/ah watchlist` shows watched items | Retention mechanic | `xa_watchlist` table or `watchlist.yml` | Limit size by permission tier | Low |
| 4 | **Configurable Duration Tiers** | Duration selector (1h, 6h, 12h, 24h, 7d) with permissions/fees | VIP perk, fee revenue | `duration-tiers` config, selection submenu | Direct — sell longer tiers | None |
| 5 | **Bulk Cancel / Collect** | "Collect All" and "Cancel All" buttons in `PlayerSellingMenu` | Players with 30 listings waste time clicking each | Batch SQL `UPDATE` | None (UX rating) | Medium — must batch |
| 6 | **Auction Re-list Button** | "Re-list at same price" on expired auctions | Reduces friction, increases volume | `AuctionService.createAuction` pre-populated | None | One extra save |
| 7 | **Admin Force-Expire Command** | `/ah admin expire <player>` or `<auctionId>` | No moderation tools exist today | New admin subcommand | None | None |
| 8 | **Configurable Item Display Formatting** | YAML template for auction lore with MiniMessage support | Hardcoded `§b` codes can't be adjusted | Replace raw codes with `PluginConfig` templates | None | Negligible |
| 9 | **`/ah status` Command** | Text summary: active listings, pending collections, revenue | Quick info without GUI | `AuctionService.getPlayerStats(UUID)` | None | One async query |
| 10 | **Listing Grace Period** | 60-second free-cancel window after listing | Eliminates accidental listing frustration | `requestTime` field check in `cancelAuction` | None | Timestamp comparison |

---

### Medium Features (10)

| # | Feature | What it does | Why | Systems | Monetization | Performance |
|---|---|---|---|---|---|---|
| 1 | **Full Bidding System** | Competing bids over time, highest wins at expiry with outbid refunds | `AuctionType.BID` is in the model but unimplemented | `BidMenu`, `placeBid()`, DB schema additions, expiration task | Maximum — premium-only | Medium — atomic bid updates |
| 2 | **Economy Statistics Dashboard** | `/ah stats` GUI: top 10 sellers, most-traded items, price trends | Engagement and server-economy transparency | `StatsManager`, `xa_stats` table, `StatsDashboardMenu` | Admin-only perm | Medium — 5-min cache |
| 3 | **Offline Delivery Mail System** | Persistent mailbox for players who were offline when their item sold | Eliminates "lost earnings" support tickets | `xa_mail` table, `MailMenu`, login check | None | Fast — one SELECT on join |
| 4 | **Config-Driven Item Currencies** | `config.yml` `economy.item-currencies` for physical item denominations | `ItemEconomyProvider` is hardcoded to Emeralds | `ItemEconomyProvider` refactor, currency selector GUI | Expands addressable market | Low |
| 5 | **Transaction History Log** | Paginated time-ordered log of all purchases/sales/fees | Trust, dispute resolution | `xa_transaction_log` table, `TransactionHistoryMenu` | None | Low — append-only inserts |
| 6 | **MiniMessage Broadcast System** | Configurable sold/listed broadcasts with hover item tooltips | Current broadcasts have no hover/click/gradient support | `MessageManager` component API, `ItemComponent` builder | None | Negligible |
| 7 | **Timed Server Auction Events** | Admin-triggered events: fee waiver, custom GUI header, broadcast | Recurring activity spikes, retention | `AuctionEventManager`, one boolean check in `createAuction` | High — sell event starter | One boolean check |
| 8 | **Advanced Search (Enchant / Price Range)** | `enchantment:sharpness,level:5`, `price:<1000`, keyword filtering | Current search only checks name and material | `SearchParser`, `SearchCriteria` evaluator | None | Medium — needs pagination fix first |
| 9 | **Auction Voucher System** | Physical item representing pending earnings; tradeable, redeemable | Unique differentiator not in any other AH plugin | Custom NBT tag, `ItemInteractListener`, `redeemVoucher()` | High — novel feature | Low — NBT check on interact |
| 10 | **GUI Layout Hot-Reload** | `/ah admin reload layouts` applies YAML menu changes live | `LayoutLoader` infrastructure already exists | `LayoutLoader.reloadLayouts()`, reload command hook | None | File I/O on admin command only |

---

### Large System Expansions (5)

**1. Full Bidding Engine with Real-Time Countdown**  
Complete bid-auction system including configurable starting price, buyout price (BIN override), auto-bid (proxy bidding), outbid notifications via titles/sounds, live countdown timer in menu title, and automated resolution on expiry.  
- **Systems:** `AuctionService.placeBid()`, `BidResolutionTask` (every 30s), `BidAuctionMenu`, DB schema additions (`current_bid`, `current_bidder_uuid`, `min_bid_increment`), outbid refund flow, bid escrow  
- **Monetization:** Maximum — can be sold as premium tier access  
- **Performance:** Heavy — bid resolution requires ordered SELECT + atomic UPDATE per expiring auction per tick

**2. Multi-Server Network Market (Redis Pub/Sub + Shared SQL)**  
Cross-server auction house where players on Server-A buy items listed by players on Server-B. Synchronized via Redis pub/sub for invalidation and a shared DB. Item delivery via a cross-server mailbox.  
- **Systems:** Wire `NetworkSyncManager` into `initializeManagers()`. Full message protocol (CREATED, SOLD, CANCELLED, EXPIRED). Redis config section. `xa_cross_server_mail` table with delivery-on-join logic  
- **Monetization:** Highest — network mode is a separate premium SKU  
- **Performance:** Critical — all writes must publish to Redis; GUI reads must go through cache, never direct to DB

**3. GUI Theme & Skin System**  
Complete theming system where every menu's visual appearance is defined in YAML `themes/` files. Ships with 3–5 built-in themes (Default, Dark, Minimal, Medieval). Server admins install/switch themes without code changes.  
- **Systems:** `ThemeManager` loading from `themes/<name>.yml`. Per-slot-type material overrides (`BORDER`, `PAGINATION_PREV`, `PAGINATION_NEXT`, `BACK`, `CONFIRM`, `CANCEL`). `BaseMenu.buildMenu()` queries `ThemeManager`  
- **Monetization:** High — sell premium theme packs, community marketplace  
- **Performance:** Theme resolution at menu-build time, pure in-memory map lookup

**4. Comprehensive Admin Control Panel GUI**  
New `/ah admin` GUI providing: live statistics, force-expire/delete any listing, player blacklist management, live fee/tax config editor, emergency "purge expired" button, and audit log of admin actions.  
- **Systems:** `AdminMenu`, `AdminAuctionListMenu`, `AdminPlayerSearchMenu`, `AdminConfigMenu`, `xa_audit_log` table  
- **Monetization:** Medium — expected in premium plugins  
- **Performance:** Admin queries can be heavier since they run infrequently

**5. Economy Sink / Auction Points Reward System**  
Every completed sale awards buyer and seller small amounts of plugin-internal "Auction Points." Points accumulate and are spent in an "Auction Rewards Shop" GUI for fee discounts, exclusive items, or cosmetic listing badges.  
- **Systems:** `xa_points` table, `AuctionPointsManager`, points awarded in `AuctionServiceImpl` post-sale, `RewardsShopMenu`, config section `rewards.points-per-transaction`  
- **Monetization:** Medium — rewards shop cosmetics, listing badges  
- **Performance:** One `UPDATE xa_points SET points=points+N WHERE player_uuid=?` per sale

---

## SECTION 3 — Competitive Improvements

### Market Comparison

| Feature | xAuctions | Competitors |
|---|---|---|
| BIN Listings | ✅ | ✅ |
| Bidding | ❌ Stub only | ✅ All paid plugins |
| Folia Support | ✅ | ❌ Most don't |
| Multi-currency | ✅ Partial | ✅ |
| Network Sync (Redis) | ❌ Dead code | ❌ Rare |
| GUI Themes | ❌ Partial | ✅ Premium |
| Price History | ❌ | ✅ Premium |
| Admin Panel | ❌ | ✅ All paid |
| MiniMessage | ❌ Uses §-codes | ✅ Modern plugins |
| Transaction Log | ❌ | ✅ Some |
| Bid Proxy / Auto-bid | ❌ | ✅ Auctioneer |

> **Key advantage:** Folia support is the single biggest competitive differentiator — zero competing mass-market AH plugins support it. Market this aggressively.

---

### Features Needed for Premium Tier

1. Full bidding engine — non-negotiable
2. MiniMessage/Adventure API throughout — `§`-codes look amateurish in 2025 plugin listings
3. Cross-server network mode — unique even among paid plugins
4. Config file validator at startup with clear error messages
5. Per-world auction house instances (RPG servers)
6. Built-in item value estimator (simple lookup table `DIAMOND=1000`)

---

### UI/UX Improvements

1. Replace every `§` color code with MiniMessage. Use `Component` for hover tooltips and clickable messages.
2. Time remaining lore with color progression: `§a2d 3h` → `§e4h 30m` → `§c15m` shifts to red as expiry approaches.
3. "ENDING SOON" badge on listings expiring within 1 hour.
4. Empty state handling in all menus — show a centered "No auctions found" item with instructions instead of a blank inventory.
5. Title feedback on successful purchase (`§a✔ Purchased!`) with a particle burst.
6. Search history cycling through last 5 queries (stored in `PlayerPreferences`).
7. "Recently Sold" feed — dedicated menu tab showing the last 10 completed transactions server-wide.

---

### Retention Mechanics

1. **Daily login streak bonus** — consecutive AH usage days grant progressive fee discounts (Day 1: 5% off, Day 7: 20% off).
2. **Seller level system** — cumulative sales volume unlocks "Trusted Seller" badge, increased limits, reduced fees.
3. **"Hot Item" algorithm** — items with 3+ recent purchases get a `§6★ HOT` tag in their lore.
4. **Action bar notification on sale** — notify seller immediately even when not in the AH GUI.
5. **Referral system** — invite a new player → receive a one-time "Listing Voucher" (free listing fee).

---

### Gamification Additions

1. **Achievement system** — "First Sale", "Power Seller (100 sales)", "Bargain Hunter (10 purchases)", "Rare Dealer (sold 1M+)". Dedicated `/ah achievements` GUI.
2. **Economy leaderboard** — `/ah top` shows top sellers by total revenue this week/month.
3. **Mystery listings** — occasional auctions with hidden item type. Buyers see only category and price range.
4. **Flash Sale events** — admin-triggered 10-minute price drops on specific items. Server broadcast drives traffic spikes.

---

## SECTION 4 — Optimization Suggestions

### Async Improvements

**Critical — `loadActiveAuctions().join()` in `AuctionsMenu.getData()`:**  
The `getData()` → `update()` → `onOpen()` → `openMenu()` chain must never call `.join()`. Pattern to adopt everywhere:

```java
@Override
public void onOpen(Player player) {
    showLoadingState(); // Fill inventory with gray glass + "Loading..." item
    plugin.getStorageProvider().loadActiveAuctions()
        .thenAccept(auctions -> {
            this.cachedAuctions = auctions;
            SchedulerUtils.run(plugin, player, this::update); // Back to player's region thread
        });
}
```

**Non-blocking auto-claim on join:**  
`PlayerConnectionListener.onJoin` must dispatch `eco.deposit()` back to the player's owning thread via `platformAdapter.runEntityTask(player, ...)`. Economy operations must never run on async threads.

**Dirty-flag batch writes for JSON:**  
Replace immediate `saveToFile()` on every mutation with a dirty flag. Flush to disk on a periodic async task (every 30 seconds) and on shutdown. Reduces I/O from O(mutations) to O(1) per time window.

---

### Caching Improvements

| Cache | TTL | Description |
|---|---|---|
| SQL active auction list | 30 seconds | `volatile List<Auction> cachedActiveAuctions` + `lastRefreshed` timestamp. Invalidate on any write. |
| Per-player auction list | 10 seconds | Throttle `AuctionHistoryMenu` re-queries |
| Price history per material | 5 minutes | `Map<Material, double[]>` — never query on demand |
| Economy balance per player | 2 seconds | Wrap `EconomyProvider.has()` to reduce Vault lock contention |

---

### Memory Optimization

1. **Migrate item serialization** from `BukkitObjectOutputStream` Base64 to Paper's `ItemStack.serializeAsBytes()` (available in Paper 1.20.4+). More compact binary format, officially supported for persistence.
2. **Evict collected/expired auctions from cache** after 5 minutes. `ItemStack` NBT data (several KB each) has no reason to remain in memory after collection.
3. **Fix `GuiManager.openMenus` memory leak** — add a `PlayerQuitEvent` handler that removes the player's menu reference. A player who disconnects without closing inventory can cause the `AbstractMenu` to live forever.
4. **Cache the `PlatformAdapter` fallback instance** in `SchedulerUtils` as a static field rather than constructing it on every invocation.

---

### Database Optimization

1. **Paginated queries — highest priority.** Replace `loadActiveAuctions()` with `loadActiveAuctionsPage(int page, int pageSize, SortOrder order)` using `LIMIT ? OFFSET ?`. Add a `COUNT(*)` overload for page count.
2. **Add `item_type` column.** Index it for O(log n) category filtering in the DB rather than O(n) in Java.
3. **Add `updateAuctionStatus()` method.** Avoid re-serializing the `ItemStack` into Base64 on every status update — only touch status columns (`is_sold`, `is_collected`, `buyer_uuid`).
4. **Complete HikariCP configuration.** Add `connectionTimeout`, `idleTimeout`, `maxLifetime`, and `minimumIdle`. Without these, connections silently go stale on MySQL servers with `wait_timeout` enabled.
5. **Switch `deleteAuction` to soft-delete.** Add `is_deleted BOOLEAN DEFAULT FALSE` column. Hard-delete makes transaction logs and admin audit features impossible.

---

### Event Handling Improvements

### Economy Integration

- Automatically detect and hook DonutCore's economy service (via Vault or direct ServicesManager lookup) and offer a "donutcore" provider configuration option. Added delayed re-checks and plugin-enable listener to handle late registrations. Avoid spamming missing-economy warnings.

### Event Handling Improvements

1. Add `@EventHandler(ignoreCancelled = true)` to `GuiManager.onClick`.
2. Add early return in `GuiManager.onClick` if the top inventory is a `PlayerInventory` (the most common non-AH case).
3. Replace `AsyncPlayerChatEvent` in `InputListener` with Paper's `AsyncChatEvent`.

---

### Folia Region Thread Fixes

1. **`PlayerConnectionListener.onJoin`** uses `Bukkit.getScheduler().runTaskLater(...)` at the bottom of the method. This is the Bukkit scheduler which doesn't exist in Folia. Replace with `platformAdapter.runEntityTaskLater(player, ..., delay)`.
2. **`AuctionHistoryMenu.update()`** calls `SchedulerUtils.run(plugin, ...)` using the global scheduler. Since this is a player-triggered menu, use `SchedulerUtils.run(plugin, player, this::render)` to ensure execution in the player's region.
3. **`NetworkSyncManager.handleMessage`** calls `storageProvider.invalidateCache(uuid)` directly in the Redis subscription thread. For the JSON backend, this calls `loadFromFile()` which modifies the `ConcurrentHashMap`. All work in the `onMessage` callback touching plugin state must dispatch to the global region scheduler.
4. **`DiscordWebhook`** — any callback that touches Bukkit APIs must route back through the region scheduler.

---

## SECTION 5 — Code Modernization

### Refactor Suggestions

1. **Eliminate `AuctionManager` wrapper entirely.** Delete it. Update `XAuctionsPlugin` to expose `getAuctionService()` as the only entry point. Update all callers (`AuctionHistoryMenu`, `PlayerSellingMenu`) to use the service directly.
2. **Replace all `§` color codes with MiniMessage.** Use `Component` API for all player-facing text. Replace every `"§a..."` string with `MiniMessage.miniMessage().deserialize("<green>...")`.
3. **Give `Auction` a sealed type hierarchy.** Consider `sealed interface Auction permits BinAuction, BidAuction`. Enables exhaustive `switch` on type instead of `if (type == BID)` chains.
4. **Extract `formatTime(long millis)` into `TimeUtils`.** Duplicated in `AuctionsMenu`, `PlayerSellingMenu`, and `AuctionHistoryMenu`.
5. **Fix `Result` API.** Add `getErrorOrNull()` and `getValueOrNull()` that return `null` instead of throwing. Defensive null-checks become possible without try/catch.

---

### Design Patterns to Adopt

| Pattern | Where | Benefit |
|---|---|---|
| **Observer / Event Bus** | `AuctionServiceImpl` → decouple from all managers | Service fires `PostBuyEvent`; interested managers react. No more `plugin.getTaxManager()` chains |
| **Strategy** | `ItemSerializer` | `ItemSerializationStrategy` interface with `BukkitObjectStreamStrategy`, `PaperBytesStrategy`, `JsonNbtStrategy` implementations |
| **Command pattern** | Admin actions | Each admin action is a separate `AdminAction` class. Enables undo history and audit logging |
| **Template Method** | `PaginatedMenu` | Add `onEmpty()` overridable method with a default "No auctions found" item |

---

### Dependency Injection

Replace `plugin.getX()` everywhere with a manual `PluginContext` record:

```java
public class PluginContext {
    private final PluginConfig config;
    private final StorageProvider storage;
    private final EconomyManager economy;
    private final MessageManager messages;
    private final TaxManager tax;
    private final ListingLimitManager limits;
    // ... all managers

    public PluginContext(PluginConfig config, StorageProvider storage, ...) {
        // Validate non-null in constructor
    }
}
```

Pass `PluginContext` to every manager/menu/service constructor. Breaks circular dependency on the plugin singleton. Enables isolated unit testing by mocking `PluginContext`.

---

### Modularization

1. **Extract `economy/` as a standalone `economy-api` module.** `EconomyProvider` is generic enough to be a separate JAR other plugins depend on.
2. **Extract `gui/framework/` as a standalone `gui-framework` module.** `BaseMenu`, `PaginatedMenu`, `MenuItem`, `MenuAction`, `ItemBuilder` are fully generic and not AH-specific. Can be released/sold independently.
3. **Separate storage backends** into `storage-json` and `storage-sql` Maven modules assembled into the final JAR. Swap storage backends without modifying core.

---

### API Exposure

1. Add a no-op `DisabledAuctionService` returned when the plugin is disabled, instead of throwing `NPE` from `XAuctionsAPI.getInstance()`.
2. Expose a read-only `AuctionStorage` interface in the API module (`loadActiveAuctions`, `loadPlayerAuctions`).
3. Add missing API events: `AuctionExpireEvent`, `AuctionCancelEvent`, `AuctionCollectEvent` to provide full coverage alongside existing `AuctionCreateEvent` and `AuctionBuyEvent`.
4. Add `@ApiStatus.Experimental` (JetBrains annotations — already in pom) to unstable API surfaces.

---

## SECTION 6 — Security Review

### Exploit Risks

**1. TOCTOU in `createAuction` — listing limit bypass.**  
The listing limit check queries `loadPlayerAuctions()` which is async. Between the count returning and `saveAuction()` completing, a player can fire multiple concurrent `createAuction` requests (double-clicking the sell button). All requests pass the limit check simultaneously since none have been saved yet.  
**Fix:** Maintain a per-player `AtomicInteger inFlightCreations` counter. Increment before the check, decrement after save or failure.

**2. Concurrent buy + collect item duplication.**  
Player A's `buyAuction` atomic DB update succeeds (`is_sold=true`). Before `updateAuction` persists `is_collected=true`, the seller opens `PlayerSellingMenu` and clicks collect on the same auction (locally showing `isSold() = false` on the cached reference). The item is returned to the seller AND given to the buyer.  
**Fix:** `collectAuction` must always re-fetch the auction from storage before returning the item. Never trust the passed-in `Auction` object.

**3. Negative / zero price listings.**  
`AuctionRequest.price` is a `double` with no guard in `SellCommand` or `AuctionServiceImpl` checking `price > 0`. The `min-price` config value is read but never validated against in `createAuction`.  
**Fix:** Add `if (request.price() < config.getAuctionSettings().minPrice()) return Result.error(...)` at the top of `createAuction`.

**4. Item loss when buyer's inventory is full.**  
If the buyer's inventory is full, the item is dropped at `buyer.getLocation()`. If the buyer is above the void, the item is permanently lost. No fallback to a pending-collection mailbox exists.  
**Fix:** If `addItem()` returns a non-empty leftover map, store the item in the persistent mail system rather than dropping it in the world.

**5. Lost seller earnings on `deposit()` failure.**  
In `collectSellerPaymentOnline`, `auction.setCollected(true)` is called and the auction is then updated in storage regardless of whether the `deposit()` call succeeded.  
**Fix:** Check `sellerEco.deposit(seller, finalAmount)` return value. If `false`, do NOT mark collected and return `Result.error("Failed to transfer funds")`.

---

### Permission Bypass Risks

1. **`AuctionsCommand` has no permission check.** Even if a server admin revokes `xauctions.use`, the command still opens the main menu. Add an explicit guard: `if (!player.hasPermission("xauctions.use")) { ... return; }`.
2. **`xauctions.bypass.antidupe` is not listed in `plugin.yml`.** Unlisted permissions are invisible to permission managers. Add it to the permissions section.
3. **`ListingLimitManager` iterates all effective permissions** even when `xauctions.limit.*` wildcard is already matched. Add the wildcard check first and return early to avoid the unnecessary scan.

---

### Duplication Risks

1. **`AntiDupeManager` is a stub.** The only dupe protection is the SQL atomic `attemptBuy`. No NBT-based item fingerprinting exists to detect laundered items relisted by alts.
2. **`cancelAuction` sets `expireTime = 0` non-atomically.** If a concurrent buyer's `attemptBuy` fires simultaneously with a cancel on the JSON provider, both operations can succeed — item returned to seller AND given to buyer, because the `synchronized (auctionCache)` block is not held across the cancel flow.
3. **Off-thread `eco.deposit()` in `PlayerConnectionListener`** can produce double-credit depending on the underlying economy plugin's thread model.

---

### Data Corruption Risks

1. **`JsonStorageProvider.saveToFile()` is not atomic.** Direct `FileWriter` to `auctions.json` means a JVM crash mid-write produces a corrupt file. Fix: write to `auctions.json.tmp`, then `Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)`. This is already correctly done in `createBackup()` but not in `saveToFile()`.
2. **`PlayerBlacklistManager.save()` calls `plugin.saveConfig()`** which serializes the entire in-memory `FileConfiguration`. If any other thread reads/writes the `YamlConfiguration` simultaneously, the file can be written with partial state.
3. **`SqlStorageProvider` schema migration swallows `SQLException`.** Failed `ALTER TABLE` migrations are silently ignored. Add `plugin.getLogger().warning(...)` with the exception message to make failures visible.
4. **`ItemSerializer.fromBase64` throws `IllegalStateException` with no recovery.** A single corrupted item entry aborts the entire `loadActiveAuctions` future, making the full auction list disappear from the GUI. Deserialize items defensively: catch per-item, log a warning, skip the entry, continue loading.

---

## PRIORITIZED ROADMAP

### 🔴 Immediate — P0 (Fix Before Any Public Release)

| # | Fix | File(s) |
|---|---|---|
| 1 | Remove `.join()` in `AuctionsMenu.getData()` — use async load with loading placeholder | `AuctionsMenu.java` |
| 2 | Fix `SqlStorageProvider.init()` hardcoded credentials — read from `PluginConfig` database section | `SqlStorageProvider.java`, `PluginConfig.java` |
| 3 | Fix `eco.deposit()` called on async thread in `PlayerConnectionListener.onJoin` — dispatch via `platformAdapter.runEntityTask` | `PlayerConnectionListener.java` |
| 4 | Replace `HashSet awaitingSearch` in `InputListener` with `ConcurrentHashMap.newKeySet()` | `InputListener.java` |
| 5 | Remove synchronous `fetchRemoteVersion()` from the banner block in `onEnable` — it blocks startup for up to 4 seconds | `XAuctionsPlugin.java` |
| 6 | Add `min-price` and `max-price` validation at the top of `AuctionServiceImpl.createAuction()` | `AuctionServiceImpl.java` |

---

### 🟠 Short-Term — P1 (Before v1.2)

| # | Fix | File(s) |
|---|---|---|
| 1 | Replace `cancelAuction`'s `setExpireTime(0)` approach with a proper `is_cancelled` DB column | `AuctionServiceImpl.java`, `SqlStorageProvider.java`, `Auction.java` |
| 2 | Fix `collectAuction` — check `deposit()` return value before marking `collected=true` | `AuctionServiceImpl.java` |
| 3 | Fix `JsonStorageProvider.saveToFile()` — use atomic temp-file write pattern | `JsonStorageProvider.java` |
| 4 | Wire `NetworkSyncManager` into `initializeManagers()` OR remove Redis dead code entirely | `XAuctionsPlugin.java`, `NetworkSyncManager.java` |
| 5 | Fix `AuctionHistoryMenu.cachedData` data race — add `volatile` keyword | `AuctionHistoryMenu.java` |
| 6 | Add `PlayerQuitEvent` handler in `GuiManager` to clean up `openMenus` map | `GuiManager.java` |
| 7 | Fix `AuctionServiceImpl.countInitialized` race — use `AtomicBoolean.compareAndSet` | `AuctionServiceImpl.java` |
| 8 | Move all hardcoded `§` message strings to `messages.yml` and route through `MessageManager` | All service/listener files |
| 9 | Replace `AsyncPlayerChatEvent` with Paper's `AsyncChatEvent` in `InputListener` | `InputListener.java` |
| 10 | Move player blacklist persistence from `config.yml` to a dedicated `blacklist.yml` | `PlayerBlacklistManager.java` |
| 11 | Add `xauctions.bypass.antidupe` and fix `ListingLimitManager` wildcard early-exit | `plugin.yml`, `ListingLimitManager.java` |
| 12 | Fix `collectAuction` to re-fetch auction from storage before returning items | `AuctionServiceImpl.java` |
| 13 | Add per-player in-flight creation counter to fix TOCTOU listing limit bypass | `AuctionServiceImpl.java` |
| 14 | Add `AuctionsCommand` permission check for `xauctions.use` | `AuctionsCommand.java` |
| 15 | Fix `Folia` — replace `getScheduler().runTaskLater` in `PlayerConnectionListener.onJoin` with `platformAdapter.runEntityTaskLater` | `PlayerConnectionListener.java` |

---

### 🟡 Mid-Term — P2 (v1.2–v1.5)

| # | Feature / Fix |
|---|---|
| 1 | **Full Bidding System** (complete `AuctionType.BID` flow) |
| 2 | **Paginated DB queries** — `LIMIT/OFFSET` in `StorageProvider` |
| 3 | **SQL AuctionCache** — 30-second TTL in-process cache layer |
| 4 | **Background ExpirationTask** — sweeps and resolves expired auctions every 60s |
| 5 | **Full MiniMessage/Adventure API migration** for all player-facing text |
| 6 | **Bulk collect / cancel** in `AuctionService` and `PlayerSellingMenu` |
| 7 | **Per-Player Transaction History** — `xa_transaction_log` table + `TransactionHistoryMenu` |
| 8 | **Price History cache** + lore indicator on auction items |
| 9 | **`updateAuctionStatus()`** — avoid re-serializing `ItemStack` on status-only DB updates |
| 10 | **Soft-delete** — `is_deleted` column instead of hard `DELETE` |
| 11 | **Defensive `fromBase64`** — per-item error recovery in `mapResultSetToAuction` |
| 12 | **`/ah admin` Control Panel GUI** — statistics, force-expire, blacklist management |
| 13 | **`PlayerQuitEvent` guard** in `GuiManager` |
| 14 | **HikariCP** — add `connectionTimeout`, `idleTimeout`, `maxLifetime`, `minimumIdle` settings |
| 15 | **Sort & Filter Persistence** via `PlayerPreferences` cache |
| 16 | **Auction Re-list button** on expired listings |
| 17 | **`/ah status` command** |

---

### 🟢 Long-Term — v2.0+

| # | Feature / System |
|---|---|
| 1 | **Multi-server Redis Network Sync** — wire `NetworkSyncManager`, full message protocol |
| 2 | **GUI Theme System** — `ThemeManager`, YAML skin files, 3+ built-in themes |
| 3 | **Auction Rewards / Points System** — retention mechanic, `xa_points` table, rewards shop |
| 4 | **Timed Auction Events** — admin-triggered fee-waiver events |
| 5 | **Auction Voucher** — transferable pending-payment item with NBT tag |
| 6 | **Manual DI Context (`PluginContext`)** — decouple all classes from `XAuctionsPlugin` singleton |
| 7 | **GUI Framework extraction** — standalone `gui-framework` Maven module |
| 8 | **Per-world auction house instances** |
| 9 | **Economy Statistics Dashboard** (`/ah stats` GUI) |
| 10 | **Achievement System** — `xa_achievements` table, `/ah achievements` GUI |
| 11 | **Advanced Search Parser** — enchantment, price range, NBT keyword filters |
| 12 | **Seller Level & Badge System** — cumulative sales unlock rank display in lore |
| 13 | **Flash Sale / Mystery Listing** — gamification events |
| 14 | **Hot-Reload GUI Layouts** — complete `LayoutLoader` to all menus |
| 15 | **MiniMessage Item Broadcast** — hover `[item]` components in sold/listed broadcasts |

---

*End of document.*

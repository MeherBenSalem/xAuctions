# xAuctions — Premium Auction House

## 🛒 Overview

**xAuctions** is a professional-grade auction marketplace plugin designed for modern Minecraft servers. Built for both small SMP communities and large multi-server networks, xAuctions delivers a seamless trading experience where players can buy and sell items through an elegant GUI-based auction house. With multi-storage backend support, advanced anti-duplication protection, and flexible economy integration, this plugin provides server owners complete control over their server economy while offering players an intuitive, fast, and secure marketplace experience.

---

## ✨ Core Features

- **Instant Buy-It-Now System** — Players can list items with fixed prices for immediate purchase, eliminating waiting times and creating a fast-paced marketplace economy.

- **Advanced Search & Filtering** — Quickly locate items by name, seller, material type, or custom search queries, making large marketplaces easy to navigate.

- **Real-Time Price Validation** — Configurable minimum and maximum price limits prevent economy-breaking listings and ensure fair market values.

- **Multi-Currency Support** — Compatible with Vault economy providers (EssentialsX, CMI, etc.) and alternative systems like PlayerPoints and TokenManager for diverse economy setups.

- **Permission-Based Listing Limits** — Grant VIP, Premium, or custom player ranks higher listing capacities through flexible permission nodes, creating upgrade incentives.

- **Configurable Listing Fees & Sales Tax** — Set global or per-item listing fees and tax rates to control money flow and stabilize your server's economy.

- **Smart Item Blacklisting** — Prevent specific items or materials from being listed, protecting your economy from exploits or unwanted items.

- **Player Blacklist Management** — Restrict access to the auction house for specific players when needed for moderation purposes.

- **Automatic Expiration Handling** — Listings automatically expire after configurable durations, with items safely returnable to sellers.

- **Broadcast Notifications** — Optional server-wide announcements when players create listings or complete purchases, driving marketplace engagement.

- **Clean, Intuitive GUI** — Modern menu design with no clutter, making browsing and managing auctions effortless for all players.

---

## 👤 Seller System

### Listing Items

Players can list items instantly through the `/ah sell <price>` command or by using the GUI's sell interface. The system validates item eligibility, checks blacklists, verifies pricing limits, and deducts optional listing fees before publishing the auction.

### Duration Control

Sellers can choose listing durations based on their permission tier. Default durations are configurable (24 hours standard), with VIP and Premium permissions granting extended listing times up to 3 or 7 days. This creates meaningful rank progression while keeping the marketplace fresh.

### Seller Dashboards

The **"My Auctions"** menu provides a comprehensive overview where sellers can:
- View all active listings with remaining time
- Track sold items awaiting payment collection
- Access expired or cancelled listings for item retrieval
- See complete auction history and sales performance

### Listing Limits

Server owners control marketplace saturation through permission-based limits:
- **Default players**: Configurable base limit (e.g., 15 concurrent listings)
- **VIP tier**: Extended limits via `xauctions.limit.vip`
- **Premium tier**: Higher limits via `xauctions.limit.premium`
- **Unlimited access**: Grant infinite listings with `xauctions.limit.unlimited`

Limits are enforced before listing creation, preventing spam while rewarding donators.

### Managing Listings

Sellers maintain full control over their auctions:
- **Cancel anytime**: Remove active listings and retrieve items immediately
- **Instant notifications**: Receive alerts when items sell
- **Secure collection**: Money from sold items is safely held until the seller claims it
- **Item recovery**: Expired or cancelled listings can be retrieved through the seller dashboard

### Expired Item Protection

When listings expire, items aren't lost—they remain in the system until the seller manually retrieves them through the "My Auctions" menu. This prevents accidental item loss and gives sellers control over when to reclaim inventory space.

### Multi-Server Synchronization

For network environments, auction data can be shared across multiple servers using the built-in storage providers (MySQL/PostgreSQL), ensuring players can access the same marketplace from any server in your network.

### Anti-Duplication Protection

The seller system includes robust anti-dupe measures:
- Items are validated before listing to prevent duplicated items from entering the marketplace
- Transaction locks prevent simultaneous operations that could cause duplication
- Creative mode restrictions (configurable) block potentially infinite items from being sold
- Permission bypass available for trusted administrators with `xauctions.bypass.antidupe`

### Earnings Management

All sale proceeds are held securely until sellers actively claim them, preventing automatic balance injection and allowing players to track earnings. The system supports multiple currency types, ensuring compatibility with diverse economy configurations.

---

## 🔍 Marketplace Experience

### Browsing & Discovery

Players access the auction house via `/ah` or aliases (`/auctions`, `/auctionhouse`, `/xauctions`) to browse all active listings in an organized, paginated interface. The clean design displays up to 45 items per page with smooth navigation.

### Advanced Search

The integrated search system allows buyers to find exactly what they need:
- **Search by seller name**: Locate trusted vendors or specific players
- **Search by item type**: Find all listings of a particular material
- **Search by display name**: Discover renamed or custom items
- **Partial matching**: Smart search recognizes partial queries

### Item Information

Each listing displays comprehensive details:
- Item name, quantity, and all enchantments/attributes
- Current asking price with currency formatting
- Seller's username for reputation tracking
- Time remaining until expiration
- Clear visual indicators for auction status

### Special Item Support

The marketplace handles complex items flawlessly:
- **Shulker Boxes**: Preview contents before purchasing with dedicated preview menus
- **Enchanted items**: All enchantments displayed in item tooltips
- **Custom items**: Supports modded items, renamed items, and items with custom lore
- **Damaged items**: Configurable whether damaged tools/armor can be listed

### Instant Purchasing

Buyers can purchase items with a single click. The system:
1. Verifies the buyer has sufficient funds
2. Checks inventory space availability
3. Processes the transaction atomically (all-or-nothing)
4. Delivers the item instantly
5. Notifies both buyer and seller
6. Records the transaction for history tracking

### Price Transparency

All prices are displayed in your server's configured currency format, with automatic number formatting for readability (e.g., "$1,000,000" instead of raw numbers).

---

## ⚙ Server Owner Controls

### Complete Configurability

Every aspect of the auction system is controlled through `config.yml`:
- **Economy provider selection**: Vault, PlayerPoints, or TokenManager
- **Default listing duration**: Set how long auctions last (seconds)
- **Price boundaries**: Minimum and maximum price limits
- **Listing fees**: Charge players to create listings (prevents spam)
- **Sales tax rates**: Global or per-item tax percentages
- **Creative mode restrictions**: Block creative players from listing items
- **Damaged item policy**: Allow or prevent damaged items
- **Broadcast toggles**: Control server-wide auction announcements

### Flexible Storage Options

Choose the storage backend that fits your infrastructure:
- **JSON**: Lightweight file-based storage ideal for small-to-medium servers
- **MySQL/MariaDB**: Enterprise-grade relational database for large networks
- **PostgreSQL**: Advanced SQL database with superior performance
- **SQLite**: Embedded database for zero-configuration setups

All storage providers support asynchronous operations, ensuring zero lag impact on gameplay.

### Automatic Backups

The JSON storage provider creates periodic backups (every 5 minutes by default), safeguarding auction data against corruption or server crashes. SQL providers leverage database-native backup solutions.

### Message Customization

All player-facing messages are defined in `messages.yml`, supporting full color code customization and placeholder variables. Tailor the plugin's language to match your server's branding and style.

### Permission System

Fine-grained permission nodes allow precise control:
- **xauctions.use**: Access to the auction house GUI
- **xauctions.sell**: Ability to create listings
- **xauctions.buy**: Ability to purchase items
- **xauctions.admin**: Full administrative access
- **xauctions.fee.exempt**: Bypass listing fees
- **xauctions.limit.***: Grant unlimited listings
- Custom limit tiers via `xauctions.limit.<number>`

### Admin Commands

Powerful administrative tools include:
- `/ah admin reload`: Reload configuration without restart
- `/ah admin debug`: Enable detailed logging for troubleshooting
- `/ah admin remove <player>`: Remove any player's auction listings
- Permission-based admin access ensures security

### Blacklist Management

Two independent blacklist systems provide protection:
- **Item Blacklist**: Block specific materials from being listed (configurable via config)
- **Player Blacklist**: Prevent specific players from using the auction house
Both systems initialize automatically and persist across restarts.

### Economy Integration

The plugin automatically detects and hooks into Vault economy providers (EssentialsX, CMI, etc.) at startup. If Vault isn't found, fallback to alternative providers or manual configuration ensures compatibility across all server types.

### Data Migration Tools

Built-in conversion utilities allow seamless migration between storage backends—upgrade from JSON to MySQL as your server grows without losing auction data.

### Logging & Debugging

Enable debug mode to receive detailed console logs of all auction operations, storage queries, and transactions. Perfect for diagnosing issues or monitoring marketplace activity.

---

## 🔌 Integrations

### Vault Economy

**Full support for Vault-based economy plugins** including EssentialsX, CMI, UltraEconomy, and any other Vault-compatible provider. The plugin automatically detects your economy provider at startup and handles all currency formatting and transaction processing.

### PlaceholderAPI

**Extensive placeholder support** for displaying auction statistics in scoreboards, chat formats, and other plugins:
- `%xauctions_active_count%`: Total number of active auctions
- `%xauctions_my_active_count%`: Player's active listing count
Placeholders update in real-time and work with any PAPI-compatible plugin.

### Discord Webhooks

**Optional Discord integration** sends automatic notifications to your server's Discord channel when:
- New auctions are created
- High-value items are listed
- Sales are completed
Configure your webhook URL in the config to enable seamless community engagement.

### PlayerPoints

**Alternative currency support** via PlayerPoints plugin, allowing servers to use point-based economies instead of traditional money systems.

### TokenManager

**Token economy support** for servers using custom token systems, providing even more flexibility in how players trade.

### Multi-Server Networks

When configured with MySQL/PostgreSQL storage, auction data is shared across all servers in your network, creating a unified marketplace accessible from anywhere. Perfect for hub-and-spoke or multi-world server setups.

---

## 🛡 Security & Stability

### Anti-Duplication System

The **AntiDupeManager** prevents item duplication exploits through:
- **Transaction locks**: Players are locked during item transfers to prevent timing attacks
- **Atomic operations**: All buy/sell transactions are all-or-nothing to prevent partial failures
- **Creative mode blocking**: Configurable restriction prevents creative players from listing infinite items
- **Inventory validation**: Items are verified before and after transactions
- **Bypass permissions**: Trusted admins can bypass checks with `xauctions.bypass.antidupe`

### Data Integrity

All storage operations use **CompletableFuture** async patterns, ensuring:
- Zero main thread blocking (no lag)
- Graceful error handling
- Automatic retry logic for transient failures
- Data consistency across restarts

### Connection Pooling

SQL storage providers use **HikariCP**, the fastest and most reliable Java connection pool:
- Configurable pool sizes to match your server load
- Automatic connection recovery
- Prepared statements to prevent SQL injection
- Optimized query performance

### Graceful Shutdown

The plugin safely saves all auction data during server shutdown, preventing data loss even during unexpected crashes.

### Auction Cleanup

Expired auctions are automatically flagged and removed from active listings, preventing marketplace clutter. Items remain claimable by sellers indefinitely.

### Item Validation

Before listing, the system validates:
- Item is not blacklisted
- Item is not damaged (if configured)
- Player is not in creative mode (if configured)
- Price is within configured bounds
This prevents economy-breaking exploits and maintains marketplace quality.

### Concurrent Safety

The plugin is fully thread-safe, supporting high-concurrency environments with multiple players buying and selling simultaneously without conflicts.

---

## 🧩 Developer API

xAuctions exposes a clean, stable **public API** for third-party plugins and extensions.

### Accessing the API

```java
XAuctionsAPI api = XAuctionsAPI.getInstance();
```

### Available Services

**AuctionService** — Core auction operations:
- `createAuction(AuctionRequest)`: Programmatically create new auctions
- `buyAuction(Player, Auction)`: Process purchases
- `cancelAuction(Player, Auction)`: Cancel listings
- `collectAuction(Player, Auction)`: Handle item/money collection
- `getActiveCount()`: Retrieve total active auction count

**EconomyService** — Economy abstraction layer:
- Currency formatting
- Balance checks
- Transaction processing
- Multi-currency support

### Event System

The plugin fires custom events for external monitoring:
- `AuctionCreateEvent`: When a new listing is created
- `AuctionBuyEvent`: When an item is purchased
- `AuctionCancelEvent`: When a listing is cancelled
- `AuctionExpireEvent`: When a listing expires

All events are cancellable, allowing external plugins to implement custom logic or restrictions.

### Model Access

The API provides access to the **Auction** model with builder pattern support:
```java
Auction auction = Auction.builder()
    .auctionId(UUID.randomUUID())
    .sellerUuid(player.getUniqueId())
    .price(1000.0)
    .build();
```

### Result Pattern

All API methods return `Result<T>` objects with success/failure states and error messages, preventing exception-based flow control.

### Thread Safety

All API methods are thread-safe and use async operations where appropriate. Developers should handle `CompletableFuture` responses correctly.

---

## 🚀 Why This Plugin Stands Out

### Enterprise-Grade Architecture

Unlike basic auction plugins, xAuctions is built with **modular service layers**, **abstracted storage providers**, and **async-first design patterns**. This architecture ensures scalability from small SMPs to large networks with thousands of daily transactions.

### Zero-Lag Performance

Every database query, every transaction, and every file operation runs **asynchronously**, ensuring the auction house never causes TPS drops—even during peak marketplace activity.

### Multi-Storage Flexibility

Most auction plugins lock you into one storage type. xAuctions supports **JSON, MySQL, MariaDB, PostgreSQL, and SQLite** with hot-swappable configuration, letting you choose the right solution for your scale.

### Security-First Design

Built-in **anti-duplication protection**, **atomic transactions**, **connection pooling**, and **input validation** prevent exploits that plague other marketplace plugins. Your economy stays secure.

### Developer-Friendly API

The clean, documented public API makes xAuctions extensible. Build custom integrations, add unique features, or monitor marketplace activity without touching the core plugin code.

### Production-Ready Reliability

With automatic backups, graceful error handling, connection pool management, and comprehensive logging, xAuctions is built for **24/7 production environments**, not hobbyist servers.

### Permission-Based Monetization

The flexible permission system creates natural **upgrade paths** for player ranks. Offer VIPs more listing slots, longer durations, or fee exemptions—driving donations while enhancing gameplay.

### Network-Scale Synchronization

When configured with SQL storage, your entire network shares **one unified marketplace**, creating a centralized economy across lobby, survival, creative, and minigame servers.

### Constant Innovation

Automatic update checking keeps you informed of new features and security patches. The plugin actively evolves based on community needs and modern Minecraft server requirements.

---

**xAuctions** — Where performance meets professionalism.

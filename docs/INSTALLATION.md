# Installation

## Requirements

- Paper `1.21.11`
- Java `21`
- Optional: Vault, only if other plugins need Vault economy compatibility
- Optional but recommended: LuckPerms

EconomyShopGUI and a separate economy provider are not required.

## Install The Plugin

Copy the jar into the server:

```text
plugins/FallenEconomy.jar
```

Start the server once so the plugin creates:

```text
plugins/FallenEconomy/config.yml
plugins/FallenEconomy/buy-shop.yml
plugins/FallenEconomy/sell-values.yml
plugins/FallenEconomy/auctions.yml
plugins/FallenEconomy/orders.yml
plugins/FallenEconomy/balances.yml
```

Then stop the server before editing config or data files.

## Recommended Setup Order

1. Install `FallenEconomy.jar`.
2. Start the server once.
3. Stop the server.
4. Edit `plugins/FallenEconomy/config.yml`, `buy-shop.yml`, or `sell-values.yml` if needed.
5. Add permissions with LuckPerms.
6. Start the server again.
7. Test `/shop`, `/sell`, `/balance`, `/pay`, `/ah`, and `/order`.

## Package Setup

When using the full Fallen Economy package, copy the contents of:

```text
server-root/plugins/
```

into the server `plugins/` folder.

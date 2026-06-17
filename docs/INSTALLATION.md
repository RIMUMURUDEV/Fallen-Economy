# Installation

## Requirements

- Paper `1.21.11`
- Java `21`
- Optional: Vault plus an economy provider
- Optional but recommended: LuckPerms

Vault is not required for basic testing. When Vault is missing, Fallen Economy uses its internal balance file.

## Install The Plugin

Copy the jar into the server:

```text
plugins/FallenEconomy.jar
```

Start the server once so the plugin creates:

```text
plugins/FallenEconomy/config.yml
plugins/FallenEconomy/auctions.yml
plugins/FallenEconomy/orders.yml
plugins/FallenEconomy/balances.yml
```

Then stop the server before editing config values.

## Recommended Setup Order

1. Install `FallenEconomy.jar`.
2. Start the server once.
3. Stop the server.
4. Edit `plugins/FallenEconomy/config.yml`.
5. Add permissions with LuckPerms.
6. Start the server again.
7. Test `/ah`, `/ah sell <price>`, `/order`, and `/order create <unitPrice> <amount>`.

## Package Setup

When using the full Fallen Economy package, copy:

```text
server-root/plugins/FallenEconomy.jar        -> plugins/FallenEconomy.jar
server-root/plugins/FallenEconomy/config.yml -> plugins/FallenEconomy/config.yml
```

The full package also includes EconomyShopGUI configs, but this plugin can run by itself.

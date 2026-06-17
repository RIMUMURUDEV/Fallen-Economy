# Fallen Economy Plugin

Custom Paper `1.21.11` plugin for the Fallen Economy package.

It provides a lightweight auction house and buy-order system without needing a third-party auction plugin. If Vault is installed and has an economy provider, Fallen Economy uses it. If Vault is missing, the plugin falls back to internal `Essence` balances.

## Features

- `/ah` auction house GUI
- `/ah sell <price>` for listing the held item stack
- auction purchase confirmation GUI
- auction price limits, listing limits, and sorting
- `/order` buy-order GUI
- `/order create <unitPrice> <amount>` for funded buy orders
- `/order fill <id> [amount]` for selling matching held items into orders
- Vault economy support
- internal balance fallback and `/feconomy` admin tools
- YAML persistence for auctions, orders, and internal balances

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Commands](docs/COMMANDS.md)
- [Permissions](docs/PERMISSIONS.md)
- [Configuration](docs/CONFIGURATION.md)
- [Auction House](docs/AUCTION_HOUSE.md)
- [Buy Orders](docs/BUY_ORDERS.md)
- [Economy](docs/ECONOMY.md)
- [Data Files](docs/DATA_FILES.md)
- [Build](docs/BUILD.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)

## Quick Build

From the workspace root:

```text
powershell -ExecutionPolicy Bypass -File tools\build_fallen_economy_plugin.ps1
```

The built jar is copied to:

```text
Fallen Economy\server-root\plugins\FallenEconomy.jar
```

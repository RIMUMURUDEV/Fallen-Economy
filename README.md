# Fallen Economy Plugin

Standalone Paper `1.21.11` economy plugin for the Fallen Economy package.

`FallenEconomy.jar` provides Essence balances, native `/shop`, native `/sell`, auctions, and buy orders without EconomyShopGUI or an external economy provider. Vault is optional compatibility only: if `Vault.jar` is installed, Fallen Economy exposes its internal Essence balances as a Vault economy provider for other plugins.

## Features

- `/shop` and `/buy` native buy-only server shop
- `/buy config` admin editor for adding/removing buy-shop items
- `/sell`, `/sell hand`, and `/sell all` native selling
- `/balance`, `/bal`, `/money`, and `/pay`
- `/ah` auction house with `/ah sell <price>`
- `/order` buy-order GUI and funded item orders
- internal Essence balances stored in `balances.yml`
- optional Vault provider registration
- YAML persistence for shop, sell values, auctions, orders, and balances

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Commands](docs/COMMANDS.md)
- [Permissions](docs/PERMISSIONS.md)
- [Configuration](docs/CONFIGURATION.md)
- [Buy Shop](docs/BUY_SHOP.md)
- [Sell Values](docs/SELL_VALUES.md)
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

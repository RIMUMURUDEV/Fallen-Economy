# Fallen Economy Plugin

Standalone Paper `1.21.11` economy plugin for the Fallen Economy package.

`FallenEconomy.jar` provides a native `$` economy, `/shop`, `/sell`, auctions, buy orders, and a separate PlayerPoints-backed Essence shop. EconomyShopGUI is not required. Vault is optional compatibility only: if `Vault.jar` is installed, Fallen Economy exposes `$` as the Vault economy provider.

## Features

- `/shop` native shop with End, Nether, Gear, Food, and Spawners categories
- End, Nether, Gear, and Food use `$`; Spawners use PlayerPoints Essence
- `/essenceshop` direct/admin access for PlayerPoints Essence items, starting with spawners
- `/sell`, `/sell hand`, and `/sell all` native selling into `$`
- `/balance`, `/bal`, `/money`, and `/pay` for `$`
- `/essence` for PlayerPoints Essence balance
- `/ah` auction house and `/order` funded buy orders using `$`
- internal `$` balances stored in `money.yml`
- optional Vault provider registration for `$`
- YAML persistence for shop, sell values, essence shop, auctions, orders, and money

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

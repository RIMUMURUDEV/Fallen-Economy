# Fallen Economy

Fallen Economy is a Paper 1.21.11 economy/shop setup for a PvP-focused survival server.
It is now fully standalone: one `FallenEconomy.jar` provides Essence balances, shop, selling, auctions, and buy orders.

## Included

- `server-root/plugins/FallenEconomy.jar`: standalone Fallen Economy plugin.
- `server-root/plugins/FallenEconomy/config.yml`: main settings.
- `server-root/plugins/FallenEconomy/buy-shop.yml`: native buy-only shop items.
- `server-root/plugins/FallenEconomy/sell-values.yml`: native sell values for survival-obtainable items.
- `docs/`: full setup and maintenance documentation.

## Core Behavior

- `/shop` and `/buy`: native buy-only PvP/rare/supply server shop.
- `/buy config`: admin buy-shop editor for adding/removing items and setting prices.
- `/sell`: native sell-values GUI.
- `/sell hand`: sells the held stack.
- `/sell all`: sells sellable storage inventory items, without armor/offhand.
- `/balance`, `/bal`, `/money`: shows Essence balance.
- `/pay <player> <amount>`: sends Essence to another online player.
- `/ah`: auction house from `FallenEconomy.jar`.
- `/ah sell <price>`: lists the held item stack.
- `/order`: buy orders from `FallenEconomy.jar`.
- `/order create <unitPrice> <amount>`: creates an item order using the held item type.
- Currency display name: `Essence`.
- Sell values are native and generated from Paper API 1.21.11 materials.
- Sell values are coefficient-based: `baseValue * currencyPerPoint * optional multipliers`.
- Vault is optional compatibility only. Fallen Economy always uses its own internal Essence balances.

## Read Next

- `docs/INSTALLATION.md`: install steps.
- `docs/COMMANDS.md`: command map and aliases.
- `docs/PRICING.md`: coefficient pricing system.
- `docs/AUCTION_AND_ORDERS.md`: custom auction/order module.
- `docs/REGENERATE.md`: how to rebuild configs after price changes.

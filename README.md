# Fallen Economy

Server package for the standalone Fallen Economy plugin on Paper `1.21.11`.

`FallenEconomy.jar` now handles the normal `$` economy, shop, selling, auction house, and buy orders by itself. Essence is separate and comes from PlayerPoints for `/essence`, `/essenceshop`, spawners, keys, and future bounty integration.

## Included

- `server-root/plugins/FallenEconomy.jar`
- `server-root/plugins/FallenEconomy/config.yml`
- `server-root/plugins/FallenEconomy/buy-shop.yml`
- `server-root/plugins/FallenEconomy/essence-shop.yml`
- `server-root/plugins/FallenEconomy/sell-values.yml`

## Currency Split

- `$`: internal `money.yml`, used by `/shop` End/Nether/Gear/Food, `/sell`, `/ah`, `/order`, `/balance`, `/pay`, and Vault compatibility.
- `Essence`: PlayerPoints-backed, used by `/essence` and `/essenceshop`.

Vault is optional and exposes only `$`. PlayerPoints is optional for server startup, but required for Essence features.

## Main Commands

- `/shop`: normal `$` categories only.
- `/essenceshop`: direct Essence shop access, currently spawners.
- `/sell`, `/sell hand`, `/sell all`: sell items for `$`.
- `/balance`, `/bal`, `/money`, `/pay`: `$` balance and transfers.
- `/ah`: auction house using `$`.
- `/order`: buy orders using `$`.
- `/feconomy`: admin `$` and Essence management.

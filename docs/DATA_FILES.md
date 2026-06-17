# Data Files

Fallen Economy stores persistent data in YAML files inside:

```text
plugins/FallenEconomy/
```

## buy-shop.yml

Stores configured `/shop` and `/buy` shop items.

Contains:

- next buy-shop id
- item stack or simple material/amount data
- price in Essence
- creation timestamp

Do not edit this while the server is running.

## sell-values.yml

Stores native `/sell` values.

Contains:

- material name
- sell value per item in Essence

Do not edit this while the server is running.

## config.yml

Main settings file.

Created from the bundled default config on first startup.

## auctions.yml

Stores active auction listings.

Contains:

- next auction id
- seller UUID
- seller name
- serialized item stack
- price
- creation timestamp

Do not edit this while the server is running.

## orders.yml

Stores active buy orders.

Contains:

- next order id
- creator UUID
- creator name
- material
- unit price
- original amount
- remaining amount
- creation timestamp

Do not edit this while the server is running.

## balances.yml

Stores internal Essence balances.

This is always the source of truth, even when Vault compatibility is enabled.

## Backups

Before manual edits, stop the server and copy:

```text
plugins/FallenEconomy/auctions.yml
plugins/FallenEconomy/orders.yml
plugins/FallenEconomy/balances.yml
plugins/FallenEconomy/buy-shop.yml
plugins/FallenEconomy/sell-values.yml
```

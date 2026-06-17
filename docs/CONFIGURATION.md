# Configuration

Default file:

```text
plugins/FallenEconomy/config.yml
```

Default config:

```yaml
currency-name: Essence
auction:
  min-price: 1
  max-price: 1000000
  max-listings-per-player: 25
  confirm-purchase: true
  default-sort: NEWEST
orders:
  min-unit-price: 1
  max-unit-price: 1000000
  max-amount: 3456
  max-orders-per-player: 25
  default-sort: NEWEST
internal-economy:
  enabled-when-vault-missing: true
  starting-balance: 0
```

## Root

| Key | Type | Description |
| --- | --- | --- |
| `currency-name` | string | Display name used in messages and GUI lore. |

## Auction

| Key | Type | Description |
| --- | --- | --- |
| `auction.min-price` | number | Lowest allowed auction listing price. |
| `auction.max-price` | number | Highest allowed auction listing price. |
| `auction.max-listings-per-player` | integer | Maximum active auction listings per player. |
| `auction.confirm-purchase` | boolean | Whether clicking an auction opens a confirmation GUI before payment. |
| `auction.default-sort` | string | Initial auction GUI sort mode. |

## Orders

| Key | Type | Description |
| --- | --- | --- |
| `orders.min-unit-price` | number | Lowest unit price for buy orders. |
| `orders.max-unit-price` | number | Highest unit price for buy orders. |
| `orders.max-amount` | integer | Highest item amount for one buy order. |
| `orders.max-orders-per-player` | integer | Maximum active buy orders per player. |
| `orders.default-sort` | string | Initial buy-order GUI sort mode. |

## Internal Economy

| Key | Type | Description |
| --- | --- | --- |
| `internal-economy.enabled-when-vault-missing` | boolean | Enables internal balances when Vault economy is unavailable. |
| `internal-economy.starting-balance` | number | Default balance for players not yet present in `balances.yml`. |

## Sort Values

Use one of:

```text
NEWEST
OLDEST
PRICE_ASC
PRICE_DESC
AMOUNT
```

Lowercase values also work through command parsing, but config should use uppercase for readability.

# Configuration

Default file:

```text
plugins/FallenEconomy/config.yml
```

Default config:

```yaml
currency-name: Essence
buy:
  min-price: 1
  max-price: 1000000
  max-items: 500
  default-sort: NEWEST
sell:
  allow-hand: true
  allow-all: true
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
  starting-balance: 0
```

## Main Keys

| Key | Type | Description |
| --- | --- | --- |
| `currency-name` | string | Display name used in messages and GUI lore. |
| `internal-economy.starting-balance` | number | Default Essence balance for players not yet present in `balances.yml`. |

## Shop And Sell

| Key | Type | Description |
| --- | --- | --- |
| `buy.min-price` | number | Lowest allowed buy-shop item price. |
| `buy.max-price` | number | Highest allowed buy-shop item price. |
| `buy.max-items` | integer | Maximum number of configured buy-shop items. |
| `buy.default-sort` | string | Initial buy-shop GUI sort mode. |
| `sell.allow-hand` | boolean | Reserved setting for `/sell hand`. |
| `sell.allow-all` | boolean | Reserved setting for `/sell all`. |

Buy-shop prices are stored in `buy-shop.yml`. Sell prices are stored in `sell-values.yml`.

## Auction And Orders

| Key | Type | Description |
| --- | --- | --- |
| `auction.min-price` | number | Lowest allowed auction listing price. |
| `auction.max-price` | number | Highest allowed auction listing price. |
| `auction.max-listings-per-player` | integer | Maximum active auction listings per player. |
| `auction.confirm-purchase` | boolean | Whether clicking an auction opens a confirmation GUI before payment. |
| `auction.default-sort` | string | Initial auction GUI sort mode. |
| `orders.min-unit-price` | number | Lowest unit price for buy orders. |
| `orders.max-unit-price` | number | Highest unit price for buy orders. |
| `orders.max-amount` | integer | Highest item amount for one buy order. |
| `orders.max-orders-per-player` | integer | Maximum active buy orders per player. |
| `orders.default-sort` | string | Initial buy-order GUI sort mode. |

## Sort Values

Use one of:

```text
NEWEST
OLDEST
PRICE_ASC
PRICE_DESC
AMOUNT
```

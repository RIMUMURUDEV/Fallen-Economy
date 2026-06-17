# Commands

## Player Commands

| Command | Description |
| --- | --- |
| `/ah` | Opens the auction house GUI. |
| `/auction` | Alias for `/ah`. |
| `/auctions` | Alias for `/ah`. |
| `/ah sell <price>` | Lists the item stack in your main hand. |
| `/ah sort <mode>` | Sets your auction sort mode. |
| `/ah cancel <id>` | Cancels one of your auction listings. |
| `/ah help` | Shows auction help. |
| `/order` | Opens the buy-order GUI. |
| `/orders` | Alias for `/order`. |
| `/order create <unitPrice> <amount>` | Creates a buy order for the item type in your main hand. |
| `/order fill <id> [amount]` | Fills a buy order with matching items from your main hand. |
| `/order sort <mode>` | Sets your order sort mode. |
| `/order cancel <id>` | Cancels one of your buy orders and refunds remaining escrow. |
| `/order help` | Shows buy-order help. |

## Admin Commands

| Command | Description |
| --- | --- |
| `/feconomy balance <player>` | Shows internal balance for a player. |
| `/feconomy give <player> <amount>` | Adds internal balance to a player. |

`/feconomy` only works with internal balances. If Vault economy is active, `/feconomy` will tell you internal balances are disabled because Vault is being used.

## Sort Modes

Both `/ah sort` and `/order sort` support:

```text
newest
oldest
price_asc
price_desc
amount
```

In the GUI, clicking the hopper cycles sort mode.

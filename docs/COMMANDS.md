# Commands

## Player Commands

| Command | Description |
| --- | --- |
| `/shop` | Opens the native buy-only shop GUI. |
| `/buy` | Opens the same native buy shop GUI. |
| `/buy sort <mode>` | Sets buy-shop sort mode. |
| `/sell` | Opens native sell-values GUI. |
| `/sell hand` | Sells the stack in your main hand. |
| `/sell all` | Sells all sellable storage-inventory items, excluding armor/offhand. |
| `/balance` | Shows your Essence balance. |
| `/bal` | Alias for `/balance`. |
| `/money` | Alias for `/balance`. |
| `/pay <player> <amount>` | Pays Essence to another online player. |
| `/ah` | Opens the auction house GUI. |
| `/auction` | Alias for `/ah`. |
| `/auctions` | Alias for `/ah`. |
| `/ah sell <price>` | Lists the item stack in your main hand. |
| `/ah sort <mode>` | Sets auction sort mode. |
| `/ah cancel <id>` | Cancels one of your auction listings. |
| `/order` | Opens the buy-order GUI. |
| `/orders` | Alias for `/order`. |
| `/order create <unitPrice> <amount>` | Creates a funded buy order for the held item type. |
| `/order fill <id> [amount]` | Fills a buy order with matching items from your main hand. |
| `/order sort <mode>` | Sets order sort mode. |
| `/order cancel <id>` | Cancels one of your buy orders and refunds remaining escrow. |

## Admin Commands

| Command | Description |
| --- | --- |
| `/buy config` | Opens the admin buy-shop config GUI. |
| `/buy config add <price>` | Adds the held item stack to the buy shop. |
| `/buy config remove <id>` | Removes a buy-shop item. |
| `/buy config price <id> <price>` | Changes a buy-shop item price. |
| `/buy config list` | Lists configured buy-shop items in chat. |
| `/feconomy balance <player>` | Shows a player's Essence balance. |
| `/feconomy give <player> <amount>` | Adds Essence to a player. |

## Sort Modes

`/buy sort`, `/ah sort`, and `/order sort` support:

```text
newest
oldest
price_asc
price_desc
amount
```

In supported GUIs, clicking the hopper cycles sort mode.

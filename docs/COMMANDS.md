# Commands

## Player Commands

| Command | Description |
| --- | --- |
| `/shop` | Opens native shop categories. |
| `/shop <end|nether|gear|food>` | Opens a `$` shop category. |
| `/shop spawners` | Opens the Essence-priced spawner category. |
| `/shop sort <mode>` | Sets shop sort mode. |
| `/essence` | Shows PlayerPoints Essence balance. |
| `/essenceshop` | Opens the Essence shop. |
| `/essenceshop <category>` | Opens an Essence shop category. |
| `/sell` | Opens native sell-values GUI. |
| `/sell hand` | Sells the stack in your main hand for `$`. |
| `/sell all` | Sells all sellable storage-inventory items, excluding armor/offhand. |
| `/balance` | Shows your `$` balance. |
| `/bal` | Alias for `/balance`. |
| `/money` | Alias for `/balance`. |
| `/pay <player> <amount>` | Pays `$` to another online player. |
| `/ah` | Opens the `$` auction house GUI. |
| `/auction` | Alias for `/ah`. |
| `/auctions` | Alias for `/ah`. |
| `/ah sell <price>` | Lists the item stack in your main hand for `$`. |
| `/ah sort <mode>` | Sets auction sort mode. |
| `/ah cancel <id>` | Cancels one of your auction listings. |
| `/order` | Opens `$` buy orders. |
| `/orders` | Alias for `/order`. |
| `/order create <unitPrice> <amount>` | Creates a funded `$` buy order for the held item type. |
| `/order fill <id> [amount]` | Fills a buy order with matching items from your main hand. |
| `/order sort <mode>` | Sets order sort mode. |
| `/order cancel <id>` | Cancels one of your buy orders and refunds remaining escrow. |

## Admin Commands

| Command | Description |
| --- | --- |
| `/shop config` | Opens the admin `$` shop config GUI. |
| `/shop config add <price> <category>` | Adds the held item stack to `buy-shop.yml` for `$`. |
| `/shop config remove <id>` | Removes a shop item. |
| `/shop config price <id> <price>` | Changes a shop item price. |
| `/shop config list` | Lists configured `$` shop items in chat. |
| `/essenceshop config` | Opens the Essence shop config GUI. |
| `/essenceshop config add <price> <category>` | Adds the held item stack to `essence-shop.yml`. |
| `/essenceshop config remove <id>` | Removes an Essence shop item. |
| `/essenceshop config price <id> <price>` | Changes an Essence shop item price. |
| `/feconomy balance <player>` | Shows a player's `$` balance. |
| `/feconomy give <player> <amount>` | Adds `$` to a player. |
| `/feconomy take <player> <amount>` | Removes `$` from a player. |
| `/feconomy set <player> <amount>` | Sets a player's `$` balance. |
| `/feconomy essence balance <player>` | Shows PlayerPoints Essence balance. |
| `/feconomy essence give|take|set <player> <amount>` | Manages PlayerPoints Essence. |

## Sort Modes

`/shop sort`, `/ah sort`, and `/order sort` support:

```text
newest
oldest
price_asc
price_desc
amount
```

In supported GUIs, clicking the hopper cycles sort mode.

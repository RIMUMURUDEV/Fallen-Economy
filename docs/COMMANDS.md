# Commands

The player-facing command set is provided by `FallenEconomy.jar`.

## Main Commands

| Command | Purpose |
| --- | --- |
| `/shop` | Opens the native buy-only server shop. |
| `/buy` | Opens the same native buy shop. |
| `/sell` | Opens native sell-values GUI. |
| `/sell hand` | Sells the held item stack. |
| `/sell all` | Sells sellable storage inventory items, excluding armor/offhand. |
| `/balance` | Shows Essence balance. |
| `/bal` | Alias for `/balance`. |
| `/money` | Alias for `/balance`. |
| `/pay <player> <amount>` | Pays Essence to another online player. |
| `/ah` | Opens auction house. |
| `/auction` | Alias for `/ah`. |
| `/ah sell <price>` | Lists held item stack. |
| `/order` | Opens buy orders. |
| `/orders` | Alias for `/order`. |

## Admin Commands

```text
/buy config
/buy config add <price>
/buy config remove <id>
/buy config price <id> <price>
/buy config list
/feconomy balance <player>
/feconomy give <player> <amount>
```

## Sort Modes

Supported sort modes:

```text
newest
oldest
price_asc
price_desc
amount
```

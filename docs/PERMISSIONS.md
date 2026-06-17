# Permissions

## Permission Nodes

| Permission | Default | Purpose |
| --- | --- | --- |
| `falleneconomy.buy` | `true` | Allows opening and buying from `/shop` and `/buy`. |
| `falleneconomy.buy.config` | `op` | Allows editing buy-shop items with `/buy config`. |
| `falleneconomy.sell` | `true` | Allows `/sell`, `/sell hand`, and `/sell all`. |
| `falleneconomy.balance` | `true` | Allows `/balance`, `/bal`, and `/money`. |
| `falleneconomy.pay` | `true` | Allows `/pay <player> <amount>`. |
| `falleneconomy.ah` | `true` | Allows opening and using `/ah`. |
| `falleneconomy.ah.sell` | `true` | Allows `/ah sell <price>`. |
| `falleneconomy.order` | `true` | Allows opening and using `/order`. |
| `falleneconomy.order.create` | `true` | Allows `/order create <unitPrice> <amount>`. |
| `falleneconomy.admin` | `op` | Allows admin actions, cancelling others' listings/orders, and `/feconomy`. |

## LuckPerms Examples

Basic player permissions:

```text
/lp group default permission set falleneconomy.ah true
/lp group default permission set falleneconomy.ah.sell true
/lp group default permission set falleneconomy.buy true
/lp group default permission set falleneconomy.sell true
/lp group default permission set falleneconomy.balance true
/lp group default permission set falleneconomy.pay true
/lp group default permission set falleneconomy.order true
/lp group default permission set falleneconomy.order.create true
```

Admin permission:

```text
/lp group admin permission set falleneconomy.admin true
/lp group admin permission set falleneconomy.buy.config true
```

## Notes

- A player can cancel only their own auctions/orders unless they have `falleneconomy.admin`.
- `falleneconomy.admin` does not grant money by itself. It only allows admin commands and admin cancellations.

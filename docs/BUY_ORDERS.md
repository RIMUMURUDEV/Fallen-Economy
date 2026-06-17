# Buy Orders

Buy orders let a player fund a request for an item type. Other players can fill that order by selling matching items into it.

Open orders:

```text
/order
```

Alias:

```text
/orders
```

## Creating Orders

Hold the item type you want to buy, then run:

```text
/order create <unitPrice> <amount>
```

Example:

```text
/order create 25 64
```

This creates an order for `64` of the held item type at `25 Essence` each.

## Escrow

The plugin withdraws the full value immediately:

```text
unitPrice * amount
```

That money is held as order escrow. Sellers are paid from this escrow when they fill the order.

## Filling Orders

Hold the matching item type and run:

```text
/order fill <id> [amount]
```

Example:

```text
/order fill 3 16
```

If no amount is provided, the plugin tries to fill `1` item. In the GUI:

- click fills `1`
- shift-click fills up to `64`

The plugin removes sold items from the seller's hand and pays the seller.

## Cancelling Orders

Players can cancel their own order:

```text
/order cancel <id>
```

The remaining escrow is refunded:

```text
remaining * unitPrice
```

Admins with `falleneconomy.admin` can cancel any order.

## Limits

Configured in:

```yaml
orders:
  min-unit-price: 1
  max-unit-price: 1000000
  max-amount: 3456
  max-orders-per-player: 25
```

## Sorting

Command:

```text
/order sort <mode>
```

Modes:

```text
newest
oldest
price_asc
price_desc
amount
```

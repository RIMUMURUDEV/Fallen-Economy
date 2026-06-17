# Buy Shop

The buy shop is a configurable server shop built into `FallenEconomy.jar`.

Players open it with:

```text
/shop
/buy
```

## Admin Config

Admins with `falleneconomy.buy.config` can open the config GUI:

```text
/buy config
```

The config GUI shows all buy-shop entries. Clicking an entry removes it.

## Adding Items

Hold the item stack you want to sell, then run:

```text
/buy config add <price>
```

Example:

```text
/buy config add 250
```

This adds the exact held item stack to `buy-shop.yml`. The item is not removed from the admin's inventory. The price is stored directly in the active currency, shown as `Essence`.

If the held stack amount is `64`, players buy `64`. If it is `1`, players buy `1`.

## Removing Items

Remove by command:

```text
/buy config remove <id>
```

or click the item inside `/buy config`.

## Changing Prices

Set a new price:

```text
/buy config price <id> <price>
```

Example:

```text
/buy config price 4 1200
```

## Listing Items

Show the first 25 configured entries:

```text
/buy config list
```

## Buying

Players click an item in `/shop` or `/buy`. The plugin withdraws the configured price and gives the configured item stack. If the inventory is full, leftovers drop at the player's location.

## Limits

Configured in:

```yaml
buy:
  min-price: 1
  max-price: 1000000
  max-items: 500
```

## Sorting

Command:

```text
/buy sort <mode>
```

Modes:

```text
newest
oldest
price_asc
price_desc
amount
```

# Troubleshooting

## Plugin Does Not Load

Check:

- server is Paper `1.21.11`
- server runs Java `21`
- `FallenEconomy.jar` is in `plugins/`
- console does not show `UnsupportedClassVersionError`

PlayerPoints is not required for startup. Without PlayerPoints, only Essence features are disabled.

## Commands Are Unknown Or Owned By Another Plugin

Expected command owners:

```text
/shop        -> FallenEconomy.jar
/sell        -> FallenEconomy.jar
/ah          -> FallenEconomy.jar
/order       -> FallenEconomy.jar
/essence     -> FallenEconomy.jar
/essenceshop -> FallenEconomy.jar
```

If another plugin captures `/shop`, keep `commands.force-shop-hook: true` in `plugins/FallenEconomy/config.yml`. FallenEconomy will handle player `/shop` before Bukkit dispatch. This does not affect `/shopgui`, `/shops`, or namespaced commands.

For other command conflicts, check `commands.yml` aliases and plugin command overrides.

## Buy Shop Cannot Be Edited

Check:

- player has `falleneconomy.buy.config`
- player is holding an item when using `/shop config add <price> <category>`
- price is inside `buy.min-price` and `buy.max-price`
- shop has not reached `buy.max-items`

## Shop Categories Are Empty

On startup, FallenEconomy logs how many buy-shop items were loaded and which file was used. If `buy-shop.yml` loads 0 items or only legacy/miscategorized items, the plugin backs it up and restores the bundled starter shop automatically.

## Essence Shop Does Not Work

Check:

- `PlayerPoints.jar` is installed and enabled
- player has `falleneconomy.essenceshop`
- item exists in `essence-shop.yml`
- player has enough PlayerPoints Essence

## Purchases Or Payments Fail

Normal shop, sell, pay, auctions, and orders use internal `$` from:

```text
plugins/FallenEconomy/money.yml
```

Give test money with:

```text
/feconomy give <player> <amount>
```

Give test Essence with PlayerPoints hooked:

```text
/feconomy essence give <player> <amount>
```

## Sell Fails

Check:

- player has `falleneconomy.sell`
- item material exists in `sell-values.yml`
- `/sell all` only scans storage inventory, not armor/offhand

## Safe Manual Data Edits

Stop the server before editing:

```text
plugins/FallenEconomy/money.yml
plugins/FallenEconomy/buy-shop.yml
plugins/FallenEconomy/essence-shop.yml
plugins/FallenEconomy/sell-values.yml
plugins/FallenEconomy/auctions.yml
plugins/FallenEconomy/orders.yml
```

Then start the server again.

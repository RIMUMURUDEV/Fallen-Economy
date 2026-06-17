# Troubleshooting

## Plugin Does Not Load

Check:

- server is Paper `1.21.11`
- server runs Java `21`
- `FallenEconomy.jar` is in `plugins/`
- console does not show `UnsupportedClassVersionError`

## `/shop`, `/sell`, `/buy`, `/ah`, Or `/order` Is Unknown

Make sure:

- plugin loaded successfully
- there is no startup exception
- another plugin is not overriding the same command

Expected command owners:

```text
/ah      -> FallenEconomy.jar
/auction -> FallenEconomy.jar alias
/shop    -> FallenEconomy.jar
/buy     -> FallenEconomy.jar
/sell    -> FallenEconomy.jar
/order   -> FallenEconomy.jar
/orders  -> FallenEconomy.jar alias
```

## Buy Shop Cannot Be Edited

Check:

- player has `falleneconomy.buy.config`
- player is holding an item when using `/buy config add <price>`
- price is inside `buy.min-price` and `buy.max-price`
- shop has not reached `buy.max-items`

## Buy Shop Purchase Fails

Check:

- player has `falleneconomy.buy`
- player has enough balance
- internal Essence balances are working
- item still exists in `buy-shop.yml`

## Sell Fails

Check:

- player has `falleneconomy.sell`
- item material exists in `sell-values.yml`
- `/sell all` only scans storage inventory, not armor/offhand

## Purchases Or Payments Fail

Fallen Economy always uses internal Essence balances. Give test balance with:

```text
/feconomy give <player> <amount>
```

If Vault is installed, check for:

```text
[FallenEconomy] Registered Fallen Economy as a Vault economy provider.
```

## Orders Cannot Be Created

Check:

- player is holding the item type they want to order
- unit price is inside configured limits
- amount is inside configured limits
- player has enough balance to fund full escrow
- player has not reached `orders.max-orders-per-player`

## Auctions Cannot Be Listed

Check:

- player is holding the item stack they want to sell
- price is inside configured limits
- player has not reached `auction.max-listings-per-player`
- player has `falleneconomy.ah.sell`

## GUI Clicks Do Nothing

Check console for errors. If there are no errors:

- confirm the player has permission
- confirm the clicked listing/order still exists
- for orders, confirm the player is holding the matching item type

## Safe Manual Data Edits

Stop the server before editing:

```text
plugins/FallenEconomy/auctions.yml
plugins/FallenEconomy/orders.yml
plugins/FallenEconomy/balances.yml
plugins/FallenEconomy/buy-shop.yml
plugins/FallenEconomy/sell-values.yml
```

Then start the server again.

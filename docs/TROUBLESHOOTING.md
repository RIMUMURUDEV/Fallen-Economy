# Troubleshooting

## Plugin Does Not Load

Check:

- server is Paper `1.21.11`
- server runs Java `21`
- `FallenEconomy.jar` is in `plugins/`
- console does not show `UnsupportedClassVersionError`

## `/ah` Or `/order` Is Unknown

Make sure:

- plugin loaded successfully
- there is no startup exception
- another plugin is not overriding the same command

Expected command owners:

```text
/ah      -> FallenEconomy.jar
/auction -> FallenEconomy.jar alias
/order   -> FallenEconomy.jar
/orders  -> FallenEconomy.jar alias
```

## Purchases Fail

If using Vault:

- install Vault
- install a Vault-compatible economy provider
- check startup log for `Using Vault economy`

If using internal economy:

- keep `internal-economy.enabled-when-vault-missing: true`
- give test balance with `/feconomy give <player> <amount>`

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
```

Then start the server again.

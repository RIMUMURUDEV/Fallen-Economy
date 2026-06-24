# Economy

Fallen Economy has two currencies.

## `$`

The normal server economy is internal and stored in:

```text
plugins/FallenEconomy/money.yml
```

Used by:

- `/shop` End, Nether, Gear, and Food categories
- `/sell`, `/sell hand`, `/sell all`
- `/balance`, `/bal`, `/money`
- `/pay`
- `/ah`
- `/order`
- Vault compatibility provider
- `/feconomy balance/give/take/set`

Vault is optional. If `Vault.jar` is installed, Fallen Economy registers `$` as the Vault economy provider. Vault never exposes Essence.

## Essence

Essence is backed by PlayerPoints.

Used by:

- `/essence`
- `/shop spawners`
- `/essenceshop`
- Essence-priced special entries such as spawners
- `/feconomy essence balance/give/take/set`
- future bounty/key integrations

If PlayerPoints is not installed, Fallen Economy still loads. Essence commands fail gracefully with a PlayerPoints-required message.

Old `balances.yml` is not converted into `money.yml`; this prevents old Essence balances from being mixed into normal `$`.

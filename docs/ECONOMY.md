# Economy

Fallen Economy always uses its own internal Essence balances.

Balances are stored in:

```text
plugins/FallenEconomy/balances.yml
```

Startup log without Vault:

```text
[FallenEconomy] Using Fallen internal Essence balances.
[FallenEconomy] Vault not found. Running standalone without Vault compatibility.
```

Startup log with Vault:

```text
[FallenEconomy] Using Fallen internal Essence balances.
[FallenEconomy] Registered Fallen Economy as a Vault economy provider.
```

## Player Commands

```text
/balance
/bal
/money
/pay <player> <amount>
```

## Admin Commands

```text
/feconomy balance <player>
/feconomy give <player> <amount>
```

These require:

```text
falleneconomy.admin
```

## Starting Balance

Players not present in `balances.yml` use:

```yaml
internal-economy:
  starting-balance: 0
```

This is not written to disk until their balance changes.

## Vault Compatibility

Vault is optional. If `Vault.jar` is installed, Fallen Economy registers itself as a Vault economy provider so other plugins can read/write Essence through Vault.

Vault does not replace the internal economy. `balances.yml` remains the source of truth.

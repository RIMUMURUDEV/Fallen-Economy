# Economy

Fallen Economy has two economy modes.

## Vault Mode

If Vault and a Vault-compatible economy provider are available, the plugin uses Vault.

Startup log:

```text
[FallenEconomy] Using Vault economy for Fallen Economy.
```

In Vault mode:

- `/ah` purchases withdraw from the buyer through Vault
- auction sellers are paid through Vault
- order creators fund escrow through Vault
- order fillers are paid through Vault
- `/feconomy` internal-balance commands are not used

## Internal Balance Mode

If Vault is missing and internal fallback is enabled:

```yaml
internal-economy:
  enabled-when-vault-missing: true
```

the plugin uses:

```text
plugins/FallenEconomy/balances.yml
```

Startup log:

```text
[FallenEconomy] Vault economy not found. Using Fallen internal balances.
```

Admin commands:

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

## Disabling Internal Fallback

Set:

```yaml
internal-economy:
  enabled-when-vault-missing: false
```

If Vault is unavailable, transactions will fail. This is useful if you want to force a real economy provider.

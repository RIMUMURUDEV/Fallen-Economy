# Sell Values

Fallen Economy includes native sell values in:

```text
plugins/FallenEconomy/sell-values.yml
```

## Commands

```text
/sell
/sell hand
/sell all
```

`/sell` opens the sell-values GUI. Clicking a material sells all matching items from the player's storage inventory.

`/sell hand` sells the full stack in the player's main hand.

`/sell all` sells every sellable item in the player's storage inventory. Armor and offhand are not touched.

## File Format

```yaml
items:
  DIRT: 1
  TOTEM_OF_UNDYING: 500
  DIAMOND: 120
```

Values are per item in Essence.

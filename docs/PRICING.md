# Pricing

Sell prices are paid in `$` and are generated from the workspace config:

```text
tools/pricing.coefficients.json
```

Formula:

```text
sell = baseValue * currencyPerPoint * materialMultiplier * matchingRuleMultipliers
```

## Fields

- `currencyPerPoint`: global coefficient. Increase this to make every generated sell value pay more `$`.
- `roundTo`: rounds generated prices to a step, for example `5` makes prices 5, 10, 15.
- `minimum`: lowest generated price.
- `baseValueOverrides`: changes the base point value before multipliers.
- `materialMultipliers`: multiplier for one exact item.
- `ruleMultipliers`: multiplier by `prefix`, `suffix`, `contains`, or exact `material`.
- `sellOverrides`: exact final sell price, skipping coefficient math.

## Examples

Double generated sell values:

```json
"currencyPerPoint": 2
```

Force totems to sell for exactly 500 `$`:

```json
"sellOverrides": {
  "TOTEM_OF_UNDYING": 500
}
```

After changes, regenerate configs or rebuild the plugin package.

## Buy Pricing

Shop buy prices are stored in `server-root/plugins/FallenEconomy/buy-shop.yml` and Essence shop prices are stored in `server-root/plugins/FallenEconomy/essence-shop.yml`.

The configured `price` is the cost of the configured item stack. If an entry is `16x End Stone` for `80`, buying `32x` through the confirmation GUI costs `160`.

# Configuration

Main file:

```text
plugins/FallenEconomy/config.yml
```

Important currency settings:

```yaml
money:
  name: $
  storage-file: money.yml
  starting-balance: 0

essence:
  enabled: true
  provider: PlayerPoints
  name: Essence

commands:
  force-shop-hook: true
```

`money` controls the internal `$` economy. `essence` controls the PlayerPoints-backed Essence hook. `commands.force-shop-hook` makes player `/shop` go to FallenEconomy before Bukkit command dispatch, which protects the shop from plugin command conflicts.

Shop limits:

| Path | Meaning |
| --- | --- |
| `buy.min-price` | Lowest allowed shop price. |
| `buy.max-price` | Highest allowed shop price. |
| `buy.max-items` | Maximum configured shop entries. |
| `buy.default-sort` | Initial shop GUI sort mode. |

Auction and order settings remain in `auction.*` and `orders.*`.

Utility tools:

| Path | Meaning |
| --- | --- |
| `tools.pickaxe.enabled` | Enables Fallen 3x3 Pickaxe. |
| `tools.pickaxe.max-extra-blocks` | Maximum extra blocks broken per pickaxe use. |
| `tools.axe.enabled` | Enables Fallen Treecapitator Axe. |
| `tools.axe.max-logs` | Maximum connected logs broken per axe use. |
| `tools.sellwand.enabled` | Enables Fallen Sell Wand. |

Editable data files:

- `buy-shop.yml`
- `essence-shop.yml`
- `sell-values.yml`

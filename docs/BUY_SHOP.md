# Buy Shop

`/shop` opens the native buy-only shop.

## Normal Shop

The normal shop uses `$` by default and is stored in:

```text
plugins/FallenEconomy/buy-shop.yml
```

Starter `$` categories:

- End
- Nether
- Gear
- Food

The player-facing `/shop` also shows a special `Spawners` category. That category is backed by `essence-shop.yml` and uses PlayerPoints Essence.

Each item stores:

- `category`
- `item`
- `price`
- `created-at`

Admin add command for normal `$` shop entries:

```text
/shop config add <price> <category>
```

Example:

```text
/shop config add 350 Gear
```

## Essence Shop

`/essenceshop` is the direct/admin view for special Essence items. It is stored in:

```text
plugins/FallenEconomy/essence-shop.yml
```

Starter category:

- Spawners

Admin add command:

```text
/essenceshop config add <price> <category>
```

Spawner shop entries use real `SPAWNER` items with `entity-type` stored through Bukkit item metadata. `Arrow of Slow Falling` uses a real `TIPPED_ARROW` with Slow Falling potion data.

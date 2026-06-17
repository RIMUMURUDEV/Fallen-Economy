# Auction House

The auction house is opened with:

```text
/ah
```

Aliases:

```text
/auction
/auctions
```

## Listing Items

Hold the item stack in your main hand and run:

```text
/ah sell <price>
```

Example:

```text
/ah sell 500
```

This lists the entire stack in your main hand. The stack is removed from your inventory immediately and stored in `auctions.yml`.

## Price Limits

The price must be between:

```yaml
auction:
  min-price: 1
  max-price: 1000000
```

If the price is outside this range, the listing is rejected and the item stays in the player's hand.

## Listing Limits

Each player can have up to:

```yaml
auction:
  max-listings-per-player: 25
```

active listings.

## Buying

Clicking an auction item opens the confirmation GUI when:

```yaml
auction:
  confirm-purchase: true
```

The buyer pays the listed price. The seller receives the money. The buyer receives the listed item. If the buyer's inventory is full, leftover items are dropped naturally at the buyer's location.

## Cancelling

Players can cancel their own listing:

```text
/ah cancel <id>
```

Admins with `falleneconomy.admin` can cancel any listing.

## Sorting

Command:

```text
/ah sort <mode>
```

Modes:

```text
newest
oldest
price_asc
price_desc
amount
```

The GUI hopper cycles the player's sort mode.

# Installation

Target server: Paper `1.21.11` with Java `21`.

## Required Plugins

- `FallenEconomy.jar`
- Optional: Vault, only for compatibility with other plugins that use Vault economy.
- Optional but recommended: LuckPerms.

EconomyShopGUI and a separate economy provider are not required.

## Copy Files

Copy the contents of:

```text
server-root/plugins/
```

into the server `plugins/` folder.

Included:

```text
plugins/FallenEconomy.jar
plugins/FallenEconomy/config.yml
plugins/FallenEconomy/buy-shop.yml
plugins/FallenEconomy/sell-values.yml
```

## LuckPerms Baseline

```text
/lp group default permission set falleneconomy.buy true
/lp group default permission set falleneconomy.sell true
/lp group default permission set falleneconomy.balance true
/lp group default permission set falleneconomy.pay true
/lp group default permission set falleneconomy.ah true
/lp group default permission set falleneconomy.ah.sell true
/lp group default permission set falleneconomy.order true
/lp group default permission set falleneconomy.order.create true
```

Admin permissions:

```text
/lp group admin permission set falleneconomy.admin true
/lp group admin permission set falleneconomy.buy.config true
```

## Verification

After install, test:

```text
/shop
/sell
/sell hand
/sell all
/balance
/pay <player> <amount>
/ah
/order
```

# Regenerating Fallen Economy

Use this when changing prices, updating the Paper jar, or rebuilding native Fallen Economy data files.

## Command

From the workspace root:

```text
node tools/generate_fallen_shop_template.mjs
```

The generator rebuilds:

- `Fallen Economy/server-root/plugins/FallenEconomy/buy-shop.yml`
- `Fallen Economy/server-root/plugins/FallenEconomy/sell-values.yml`
- `Fallen Economy/server-root/commands.yml`
- `Fallen Economy/docs`

It does not compile `FallenEconomy.jar`. After regenerating configs, rebuild the plugin jar if source changed:

```text
powershell -ExecutionPolicy Bypass -File tools\build_fallen_economy_plugin.ps1
```

## Paper Jar

Default source:

```text
C:\Users\Anwender\Downloads\paper-1.21.11-98.jar
```

Use another Paper jar by setting `PAPER_JAR`:

```text
$env:PAPER_JAR='C:\path\to\paper.jar'; node tools/generate_fallen_shop_template.mjs
```

## Zip

After regeneration, rebuild the zip:

```text
Compress-Archive -Path 'Fallen Economy' -DestinationPath 'Fallen Economy.zip' -Force
```

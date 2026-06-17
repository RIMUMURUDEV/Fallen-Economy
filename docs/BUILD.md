# Build

The repo is intentionally simple and can be built with the workspace PowerShell script.

## Requirements

- JDK 21
- Paper `1.21.11` jar available locally

Default Paper jar path used by the build script:

```text
C:\Users\Anwender\Downloads\paper-1.21.11-98.jar
```

## Build Command

From the workspace root:

```text
powershell -ExecutionPolicy Bypass -File tools\build_fallen_economy_plugin.ps1
```

Output:

```text
Fallen Economy\server-root\plugins\FallenEconomy.jar
Fallen Economy\server-root\plugins\FallenEconomy\config.yml
Fallen Economy\server-root\plugins\FallenEconomy\buy-shop.yml
Fallen Economy\server-root\plugins\FallenEconomy\sell-values.yml
fallen-economy-plugin\target\FallenEconomy.jar
```

## Custom Paper Jar

Set `PAPER_JAR` before building:

```text
$env:PAPER_JAR='C:\path\to\paper-1.21.11.jar'
powershell -ExecutionPolicy Bypass -File tools\build_fallen_economy_plugin.ps1
```

## Why There Is No Maven File

This project was built locally from the Paper jar already available in the workspace. The build script extracts Paper API libraries into a temp folder and compiles with `javac`.

## Release Artifact

The jar published in the GitHub release is:

```text
FallenEconomy.jar
```

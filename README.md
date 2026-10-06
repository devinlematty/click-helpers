# Inventory Hover Highlight

RuneLite plugin that draws a rectangle over the exact clickable area of the
inventory slot under your mouse, so you can see when an item is clickable.

Works in the normal inventory and the bank-side inventory. Fixed and resizable
modes are both supported because bounds come straight from the slot widgets.

## Config

- **Style**: outline, fill, or both
- **Outline / fill color** (with alpha)
- **Corner radius**: 0 (square) to 10 px
- **Outline width**: 1-4 px
- **Only slots with items**: skip empty slots
- **Bank inventory**: also highlight the inventory next to the bank
- **Prayers**: also highlight the hovered prayer (off by default)
- **Spells**: also highlight the hovered spell (off by default)
- **Hide when menu open**: no highlight while a right-click menu is open

## Run locally

```
./gradlew run
```

Launches RuneLite in developer mode with the plugin loaded.

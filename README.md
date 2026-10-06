# Inventory Hover Highlight

RuneLite plugin that draws a rectangle over the exact clickable area of the
inventory slot under your mouse, so you can see when an item is clickable.

Works in the inventory, the bank-side inventory, the prayer tab and the
spellbook. Fixed and resizable modes are both supported because bounds come
straight from the game's widgets.

## Config

### Appearance

- **Style**: outline, fill, or both
- **Outline / fill color** (with alpha)
- **Corner radius**: 0 (square) to 10 px
- **Outline width**: 1-4 px
- **Hide when right-click menu open**: no highlight while a right-click menu is open (off by default)

### Areas

- **Only slots with items**: skip empty inventory slots
- **Bank inventory**: also highlight the inventory next to the bank
- **Prayers**: also highlight the hovered prayer
- **Spells**: also highlight the hovered spell

### Click feedback

- **Click flash**: the clicked slot flashes and fades out (color and duration configurable)
- **Queued click marker**: a dot on the clicked slot until the next game tick;
  turns red if the same slot is clicked twice in one tick (useful for prayer flicking)
- **Tick cue**: shows progress through the current 600 ms game tick on the hover highlight
  - Pulse: brightens at the start of each tick
  - Drain: the outline shrinks as the tick runs out
  - Color: the outline goes from green to red as the tick runs out
- **Tick cue latency**: shifts the cue earlier to account for ping
- **Misclick guard**: a red X at the cursor when it is in a gap where a click does nothing

### Extras

- **Clickable grid**: outlines every area the hover highlight can fill, either in
  the panel under the mouse or in every visible panel
- **Autocast marker**: outlines the spell set to autocast; hovering it shows the
  hover highlight instead
- **Active prayer highlight**: replaces the game's active prayer glow with a gold
  highlight that follows the server-confirmed prayer state, so it does not
  flicker when clicking fast

## Run locally

```
./gradlew run
```

Launches RuneLite in developer mode with the plugin loaded.

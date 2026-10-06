# Click Helpers

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
- **Hover fade-out**: a highlight, including its tick cue, fades out over 400 ms after
  the mouse leaves, so the cue stays visible while moving between prayers
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
- **Mid-tick cue**: shows progress through the current click window on the hovered prayer
  (prayer tab only, since that is where flicking happens).
  A click reaches the server about one round trip after the client sees a tick, so
  the window closes one round trip *before* each tick the client sees.
  - Pulse: brightens at the cue position
  - Drain: the outline refills at the cue position, then shrinks
  - Color: the outline turns green at the cue position, then goes to red
- **Mid-tick cue latency**: your round-trip ping to the world (shown in the World Hopper)
- **Cue position**: where in the window every cue mode kicks off, 40% by default. Slightly
  before the middle leaves room for reaction time, while clicks that anticipate the
  beat a little early still land in the same tick
- **Dead click marker**: a red ring where you click an empty slot or a gap with no interaction

### Extras

- **Clickable grid**: outlines every area the hover highlight can fill, either in
  the panel under the mouse or in every visible panel
- **Dead areas**: faint red shading over the gaps where a click does nothing,
  in the panel under the mouse or in every visible panel
- **Autocast marker**: outlines the spell set to autocast; hovering it shows the
  hover highlight instead
- **Active prayer highlight**: replaces the game's active prayer glow with a gold
  highlight that follows the server-confirmed prayer state, so it does not
  flicker when clicking fast
- **Selected spell highlight**: a spell selected for manual casting gets the same gold
  highlight as an active prayer
- **Pending prayer state**: a dashed gold outline from your click until the server
  confirms the change (dashed only when turning on, half fill when turning off)

## Run locally

```
./gradlew run
```

Launches RuneLite in developer mode with the plugin loaded.

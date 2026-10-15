# Click Helpers

RuneLite plugin that shows exactly where you can click in the inventory, the
bank-side inventory, the prayer tab and the spellbook, and gives feedback when
you click.

- Highlights the exact clickable area under the mouse, with an optional grid of
  every clickable area and shading over dead gaps
- Click flash, a queued click marker until the next game tick, a red ring when a
  click does nothing, and a yellow ring when the game refuses a click
- Gold highlights for active prayers (from the server-confirmed state, so they do
  not flicker) and for a spell selected for manual casting
- Optional mid-tick cue on prayers to help with prayer flicking
- Miss markers, a floating click chart, and settings you can share
- Optional hiding of items you never click in a fight, and eat and drink delay shading

Fixed and resizable modes are both supported because bounds come straight from
the game's widgets. The plugin only draws overlays and never sends input. Item
hiding only changes how icons are drawn; hidden items still work when clicked.

## Settings

Settings are grouped in the same sections as the plugin's config panel.

### Hover highlight

- **Style**: outline, fill, or both
- **Outline / fill color** (with alpha)
- **Corner radius**: 0 (square) to 10 px
- **Round grid and dead areas to highlight radius**: rounds the grid and dead area shading with the
  hover highlight's corner radius (on by default). The game's clickable areas are
  square, so slot corners can look dead even though clicks there still work
- **Outline width**: 1-4 px
- **Hover fade-out**: a highlight, including its tick cue, fades out over 400 ms
  after the mouse leaves, so the cue stays visible while moving between prayers
- **Hover delay**: how long the mouse must rest on an area before it is
  highlighted (0 by default)
- **Hide when right-click menu open**: no highlight while a right-click menu is
  open (off by default)
- **Disco mode**: a random hover color every time the mouse moves to a new area

### Areas

- **Only slots with items**: skip empty inventory slots
- **Bank inventory / Prayers / Spells**: also highlight these panels
- **Clickable grid**: outlines every area the hover highlight can fill, either in
  the panel under the mouse or in every visible panel
- **Grid on empty slots**: also outline inventory slots with no item

### Click feedback

- **Click flash**: the clicked slot flashes and fades out (color and duration configurable)
- **Queued click marker**: a dot on the clicked slot until the next game tick.
  It turns red when the same slot is clicked twice in one tick, and yellow when
  you eat or drink during the delay, since the game ignores that click

### Missed clicks

- **Dead click marker**: a red ring where you click an empty slot or a gap with no interaction
- **Rejected click marker**: a yellow ring where a click reached the game but was
  refused, like equipping an item with no inventory space for the swap. Detected
  from the game's refusal message within two ticks of the click
- **Ring duration / animation**: how long both markers last, and whether they
  grow, shrink, pulse or show as an X
- **Miss dots**: a small dot stays at every dead click for a set time (15 s by
  default), so you can see where your misses cluster
- **Miss outline**: outlines the slot nearest a dead click, the one you most likely
  meant, in place of the normal outline until it fades

### Dead areas

- **Dead areas**: faint dark gray shading over the gaps where a click does nothing, in
  every visible panel (default) or only in the panel under the mouse
- Each panel (inventory, bank inventory, prayers, spells) has its own on/off and color

### Prayers and spells

- **Active prayer highlight**: replaces the game's active prayer glow with a gold
  highlight that follows the server-confirmed prayer state, so it does not
  flicker when clicking fast
- **Pending prayer state**: a dashed outline from your click until the server
  confirms the change (half fill while turning off)
- **Selected spell highlight**: a spell selected for manual casting gets the same
  highlight as an active prayer
- **Autocast marker**: outlines the spell set to autocast; hovering it shows the
  hover highlight instead

### Tick cue

- **Mid-tick cue**: shows progress through the current click window on the hovered
  prayer. A click reaches the server about one round trip after the client sees a
  tick, so the window closes one round trip *before* each tick the client sees.
  - Pulse: brightens at the cue position
  - Drain: the outline refills at the cue position, then shrinks
  - Color: the outline turns green at the cue position, then goes to red
- **Pulse color**: see-through light purple by default
- **Mid-tick cue latency**: your round-trip ping to the world (shown in the World Hopper)
- **Cue position**: where in the window every cue mode kicks off, 40% by default.
  Slightly before the middle leaves room for reaction time, while clicks that
  anticipate the beat a little early still land in the same tick
- **Cue on listed prayers / Cue prayers**: also show the cue on the listed prayers
  when they are not hovered. Protection prayers by default; `*` means every prayer

### Click chart

- **Click chart**: a floating box (Alt-drag to move it) with your clean click rate
  and one bar per 5 seconds over the last 2 minutes. Each bar stacks good clicks
  (green), double clicks (orange), refused clicks (yellow) and missed clicks (red),
  and a legend shows the count of each
- **Click chart hotkey**: shows or hides the chart

### Hide items

- **Hide non-fight items**: makes the icon of items with no fight action
  transparent, such as items with only Use, Open or Bury, and noted items.
  Clicks on them still work
- **Hide toggle hotkey**: turns the hiding on and off; when unset, hiding stays on
- **Fight actions**: inventory actions that keep an item visible (Eat, Drink,
  Wield, Wear, Equip and more by default)
- **Always show**: item names that are never hidden; `*` is a wildcard
- **Show hidden item outline / backfill**: a faint icon outline and slot shading
  where a hidden item is, each with its own toggle and color. Turn both off to make
  hidden slots look empty

### Eat and drink delay

- Shades food or potions while eating or drinking another one would be ignored.
  Items share a delay by their action: all food shares one, all potions share
  one, and karambwans have their own. Fill, drain or dim style

### Spotlight

- A light glow at the mouse that dims the panel outward, to show what is in
  reach. Radius, glow and dim colors are configurable

### Import/export settings

- **Export settings**: tick to export all plugin settings to the clipboard as
  readable JSON grouped by section, to share them
- **Import settings**: tick to import shared JSON settings from the clipboard

Each box unticks itself and the result shows in chat.

## Run locally

```
./gradlew run
```

Launches RuneLite in developer mode with the plugin loaded.

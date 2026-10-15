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
- Per-tab settings for the inventory, prayer tab and spellbook, with an option to
  keep all three in sync
- Spotlight at the mouse, gear switch practice with progress tracking, and named
  settings loadouts
- Optional hiding of items you never click in a fight, and eat and drink delay shading

Fixed and resizable modes are both supported because bounds come straight from
the game's widgets. The plugin only draws overlays and never sends input. Item
hiding only changes how icons are drawn; hidden items still work when clicked.

## Settings

Settings are grouped in the same sections as the plugin's config panel. Hover,
spotlight and miss dot settings are set per tab; see
[Inventory, prayer and spellbook tabs](#inventory-prayer-and-spellbook-tabs).

### Areas

- **Only slots with items**: skip empty inventory slots
- **Bank inventory / Prayers / Spells**: also highlight these panels
- **Clickable grid**: outlines every area the hover highlight can fill, either in
  the panel under the mouse or in every visible panel
- **Grid on empty slots**: also outline inventory slots with no item
- **Round grid and dead areas to highlight radius**: rounds the grid and dead area
  shading with each tab's corner radius (on by default). The game's clickable
  areas are square, so slot corners can look dead even though clicks there still work

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
- **Miss outline**: outlines the slot nearest a dead click, the one you most likely
  meant, in place of the normal outline until it fades
- Miss dots are set per tab

### Dead areas

- **Dead areas**: faint dark gray shading over the gaps where a click does nothing, in
  every visible panel (default) or only in the panel under the mouse
- Each panel (inventory, bank inventory, prayers, spells) has its own on/off and color.
  The bank inventory shades its whole frame around the slots
- **Cover whole prayer and spell tab**: also shades the rest of the tab, like the
  space a filter leaves empty, and counts clicks there as misses

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
  and stacked bars of good clicks (green), double clicks (orange), refused clicks
  (yellow) and missed clicks (red), with a legend of the count of each
- **Click chart hotkey**: shows or hides the chart
- **Time unit**: seconds or game ticks
- **Bar interval / bars**: how many seconds or ticks each bar covers, and how many
  bars to show; the chart covers bars times the interval (5 s x 24 bars by default)
- **Moving average**: a line of the average clicks per bar over the last few bars
- **Width / bar height**: size of the chart, for small screens. Text shortens to
  fit narrow charts
- **Legend / time labels**: hide either to make the chart smaller

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
- Follows the combo order: food, then a potion, then a karambwan can go in one
  tick. A potion blocks food for the rest of its tick; a karambwan blocks food
  for 3 ticks and potions for its tick
- Fast foods use their own delay: pizzas and pies 1 tick (2 for the second half of
  pizzas and apple, meat and redberry pies), cakes and giant crab meat 2 ticks

### Switch practice

- Lights up random empty inventory slots to click as fast as you can, like a gear
  switch. Clicks off the targets count as misses and show the usual miss markers
- Each round records reaction time, switch time (first to last click), the ticks
  the clicks spread over, and misses. Stats show above the inventory: the last
  round, the session (average and best time, 1-tick rate, accuracy, timeouts), or both
- Start and stop from the side panel or the **Practice hotkey**. Clicking an item
  still uses it, so practice with free slots
- Settings are on the side panel's Practice tab: slots per switch (1-28), pause
  between rounds with an optional random extra pause, a round time limit in
  seconds with a timer bar, rounds per session (the session stops and saves when
  done), click in numbered order, and target and hit colors
- The Practice tab also shows a chart of switch time and accuracy across your
  last 50 sessions

### Import/export settings

- **Export settings**: tick to export all plugin settings to the clipboard as
  readable JSON grouped by section, to share them
- **Import settings**: tick to import shared JSON settings from the clipboard
- Each box unticks itself and the result shows in chat
- **Side panel**: turns the Click Helpers side panel on or off

### Side panel

- **Loadouts tab**: Export and Import buttons; named loadouts that save every
  current setting and load or delete them from a dropdown; and **Randomize all
  settings**, which randomizes every setting except the click chart, hotkeys and
  text lists, each tab on its own (it turns off tab sync). Loadouts are kept in RuneLite's
  config, not in files
- **Practice tab**: switch practice start button, session stats, progress chart
  and practice settings

### Inventory, prayer and spellbook tabs

The inventory (and bank inventory), prayer tab and spellbook each have three
sections of their own:

- **Hover**: style (outline, fill or both), outline and fill colors, outline
  width, corner radius, hover fade-out, hover delay, hide when a right-click menu
  is open, disco mode with an optional color change interval, keep icon colors
  on hover, and a slot background color with icons kept on top
- **Spotlight**: a glow at the mouse that dims the panel outward. Radius (up to
  2000 px), dim and glow colors, glow size (0 for dimming only), spotlight the
  panel even when not hovered, only dim dead areas, and falloff: past the
  falloff start the gradient runs in reverse, fading the dim out and a falloff
  color in by the falloff end
- **Miss dots**: a dot at every dead click, with color, shape (circle, ring,
  square, diamond, triangle, cross, plus or heart), hollow, size and duration

**Sync tab settings** keeps all three tabs the same: changing a setting in one
tab changes it in the others, and turning it on copies the inventory settings to
the others.

## Run locally

```
./gradlew run
```

Launches RuneLite in developer mode with the plugin loaded.

# PerchWarehouse

Envoy-style looting event for Paper 26.2 (Java 25, needs EssentialsX).

Every `interval-minutes` a queue opens. When it closes, the plugin picks the warehouse that fits the number of queued players (the smallest whose `capacity` fits everyone). It teleports everyone in, freezes them for a countdown, then spawns crates. Right-clicking a crate gives the player everything inside it and sets off that rarity's firework. When time runs out (or every crate is looted), everyone is sent back and the crates are removed.

## Folder layout

```
plugins/PerchWarehouse/
├── config.yml          timings, PvP, boss bars, editor marker blocks
├── messages.yml        every message (MiniMessage)
├── warehouses/         one file per warehouse, the file name is the warehouse name
│   ├── small_warehouse.yml
│   ├── medium_warehouse.yml
│   └── large_warehouse.yml
├── loot/               one file per crate rarity, the file name is the rarity name
│   ├── common.yml
│   ├── uncommon.yml
│   └── rare.yml
├── queue.yml / state.yml   runtime data, don't edit while the server is running
└── editor-backups/         inventories of admins currently in the editor
```

The default files are only created on first install, so deleting one doesn't bring it back.

## Warehouse files (`warehouses/<name>.yml`)

```yaml
capacity: 10          # players this warehouse is built for
world: world          # set by the first marker you place in the editor

crates:
  random-spawns: 4    # how many random crate spots get a crate (guaranteed spots always do)
  rarities:           # names must match files in loot/
    common:
      weight: 100
      block: green_shulker_box
    uncommon:
      weight: 50
      block: blue_shulker_box
    rare:
      weight: 10
      block: red_shulker_box
      minimum-crate-spawns: 1   # optional
      maximum-crate-spawns: 1   # optional

markers:              # filled in by /warehouse editor
  player-spawns: []
  guaranteed: []
  random: []
```

How crates are placed each event:
1. **Spots:** every guaranteed spot, plus `random-spawns` random spots picked fresh.
2. **Minimums:** each rarity's `minimum-crate-spawns` is handed out first.
3. **The rest by weight:** rarities that hit `maximum-crate-spawns` are skipped.
4. **Shuffle:** rarities are spread across the spots, so a rare crate isn't always in the same place.

## Rarity files (`loot/<rarity>.yml`)

```yaml
display-name: "<red><bold>Rare Crate"   # shown in chat when it's opened
firework:
  type: BALL_LARGE
  colors: [RED, "#FFAA00"]
  fade: [WHITE]
  flicker: true
  trail: true
contents:              # everything in the crate, the player gets all of it
  diamonds:
    material: DIAMOND
    amount: "4-8"
  blade:
    item: "diamond_sword[enchantments={sharpness:5}]"
    name: "<red>Warehouse Blade"
    chance: 25         # optional, % chance it's in the crate
```

`common.yml` explains every option. Mistakes are logged in the console with the file and path, and the bad entry is skipped.

## Setting up a warehouse

1. `/warehouse create <name> <capacity>`, or use one of the defaults.
2. `/warehouse editor <name>`. Your inventory is saved and you get three marker blocks plus an exit item.
3. Place the markers:
   - **Player spawn** (lime): where players start. They face the way you were facing.
   - **Guaranteed crate spot** (gold): always gets a crate.
   - **Random crate spot** (light blue): `random-spawns` of these are used each event.
4. Punch a marker (or sneak + right-click it) to remove it.
5. Right-click the exit item, or run `/warehouse editor exit`.
6. `/warehouse debug` shows whether each warehouse is ready and which crates it has.

## Commands

| Command | Permission | |
|---|---|---|
| `/warehouse join` / `leave` / `info` | `warehouse.join` / `.leave` / `.info` (default: everyone) | `leave` also works mid-event |
| `/warehouse add <player>` / `kick <player>` | `warehouse.add` / `.kick` | |
| `/warehouse startevent [minutes]` | `warehouse.startevent` | open the queue now |
| `/warehouse forcestart` | `warehouse.forcestart` | close the open queue and start |
| `/warehouse forcestop` | `warehouse.forcestop` | end everything and put it all back |
| `/warehouse create <name> <capacity>` / `delete <name>` | `warehouse.create` / `.delete` | creates/deletes `warehouses/<name>.yml` |
| `/warehouse editor <name>` / `editor exit` | `warehouse.editor` | |
| `/warehouse reload` / `debug` | `warehouse.reload` / `.debug` | |

`warehouse.moderator` and `warehouse.admin` are parent nodes.

## Crash safety

- The queue is saved on every change. Relogging keeps your spot, and a restart reopens the queue (with at least 30s left).
- Where each player came from is saved before anyone is teleported. If the server dies mid-event, crates are removed on the next boot and players are sent back when they log in.
- Crates are tagged in their block data, so any stray ones are removed when their chunk loads.

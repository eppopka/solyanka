# Pending gameplay features — 0.6.0-dev

This batch adds Inventory, Item Name and Skill Progress HUDs plus XP-bar suppression. Adapted does not mean full upstream parity or in-game verification.

## Stray — not implemented

- Skybox
- Motion Blur
- Player Glow
- Nametags
- Block Outline
- Picture in Picture
- Titanium ESP
- Scoreboard
- Themed Chat
- CS2 Knife
- Held Item

## impl.zip — not implemented (21 feature objects)

- OdinDungeonWaypoints
- SecretCounter
- ShitterList
- SimonSays
- BestiaryESP
- PetDisplay
- Timer
- InvisibugESP
- TikiESP
- AutoRag
- AutoReel
- AlwaysWater
- AmbientParticles
- ChinaHat
- Cosmetics
- DvdBounce
- Halo
- ItemAura
- MoveHearts
- MoveHotbar
- RotatingItem

Mimic Chest is adapted. OdinDungeonWaypoints refers to the archive-specific route/step implementation, not the already adapted Odin waypoint module.

## Existing adaptations with remaining parity/testing work

All requested Odin and NoFrills categories have adapted implementations; not full upstream option/behavior parity. Outstanding limitations include provider availability/pricing parity, authoritative dungeon score/room coverage, full Sodium/Iris render parity, command-alias completion, and live server acceptance. See FEATURE_MATRIX.md, M4.md and archive-inventory.json for evidence and hook/asset gaps.

## Explicitly excluded

- AetherOSSettings from impl.zip — do not implement, per user request. Original source audit remains untouched for provenance.

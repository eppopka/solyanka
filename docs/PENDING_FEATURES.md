# Pending gameplay features — 1.2.0

This release adds archive Secret Counter and Always Water, following the v1.1 particles/item-motion/health-layout batch. It is not completion or runtime acceptance. Adapted does not mean full upstream parity or in-game verification.

## Stray — not implemented

- Picture in Picture
- CS2 Knife — Source 2 VPK/model/skin/animation backend still unported; requires player-owned CS2 data, not bundled Valve assets.

## impl.zip — not implemented (14 feature objects)

- OdinDungeonWaypoints
- ShitterList
- SimonSays
- BestiaryESP
- PetDisplay
- InvisibugESP
- TikiESP
- AutoRag
- AutoReel
- ChinaHat
- Cosmetics
- Halo
- ItemAura
- MoveHotbar

Mimic Chest, Stopwatch/Timer, DVD Bounce, Ambient Particles, Rotating Item, Move Hearts, Secret Counter and Always Water are adapted. OdinDungeonWaypoints refers to the archive-specific route/step implementation, not the already adapted Odin waypoint module.

## Existing adaptations with remaining parity/testing work

Secret Counter requires recognized server run/result text and existing dungeon context; command/chat acceptance is unverified. Always Water reconstructs the declared visual intent of missing external mixins; native rain splash/audio and shader-pack behavior are not replaced. See M12.md.

Archive visual limitations: native particle settings/culling apply; item maps/empty-hands/special-use paths can bypass the transform; Move Hearts editor box covers the baseline only, not extra rows/armor. In-game acceptance pending. See M11.md.

Motion Blur parity gaps: velocity reprojection, pre-entity/F5 velocity passes, refresh scaling, weighted frame blending and hybrid modes are not ported. Current modes are temporal accumulation with potential ghosting. Native GPU/Sodium/Iris acceptance pending. See M10.md.

Archive HUD parity: native font/rounded-panel approximation instead of archive custom font/glass rendering; Stopwatch resets on world change; DVD tiny-window and monotonic timing guards added. See M10.md.

Skybox limitations: shader-pack/native-sky-hook compatibility needs in-game acceptance; no terrain/world tint or match-sky-to-world link is implemented. Skyless dimensions require opt-in. See M9.md.

Held Item parity gaps: arms, silhouette framebuffer/post-outline, maps and special model renderers; native Minecraft/GPU/Sodium/Iris acceptance is pending. See M8.md.

World-visual parity gaps: native Player Glow does not include source blur styles; Nametags are player-only without upstream badges/cosmetics; Block Outline has contour/fill and a layered halo, but not framebuffer glow blur; Titanium regions are bounded source-node approximations. Scoreboard/chat use vanilla fonts, not source custom fonts; Themed Chat does not include frost blur or slide/bump animation. See M6.md and M7.md.

All requested Odin and NoFrills categories have adapted implementations; not full upstream option/behavior parity. Outstanding limitations include provider availability/pricing parity, authoritative dungeon score/room coverage, full Sodium/Iris render parity, command-alias completion, and live server acceptance. See FEATURE_MATRIX.md, M4.md and archive-inventory.json for evidence and hook/asset gaps.

## Explicitly excluded

- AetherOSSettings from impl.zip — do not implement, per user request. Original source audit remains untouched for provenance.

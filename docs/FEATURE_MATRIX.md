# Feature matrix

**Status: all upstream features below are backlog items, not functional modules in this milestone.**

The audit locates implementations and major dependencies; it does not certify behavior on Minecraft 26.2. Every requested feature is retained. Supporting files, mixins and assets must be traced before each port.

| Source | Feature | Proposed module | Integration concerns |
|---|---|---|---|
| Odin | Dungeon Waypoints | `dungeon.waypoints` | Room transforms, secrets, persistence, route packs and editor |
| Odin | Ice Fill | `solver.ice_fill` | Room coordinates, block updates and route resets |
| Odin | Blaze | `solver.blaze` | Entity health ordering and room lifecycle |
| Odin | Creeper Beams | `solver.creeper_beams` | Requires solution JSON and room-to-world transform |
| Odin | Door Highlight | `dungeon.doors` | Map scanner and world rendering |
| Odin | Map Info | `dungeon.map_info` | Dungeon map model and HUD |
| Odin | Mimic detection/highlight | `dungeon.mimic` | Entity/block evidence and run-state reset |
| Odin | Tick Timers | `dungeon.tick_timers` | Boss chat triggers, tick timebase, multiple HUD elements |
| Odin | Player Display | `hud.player_display` | Inspect source-specific player data and UI options |
| NoFrills | Chat Tweaks | `utility.chat` | Chat interception and history semantics |
| NoFrills | Command Keybinds | `utility.command_keybinds` | Input focus and server command dispatch |
| NoFrills | Command Shortcuts | `utility.command_shortcuts` | Parsing, command precedence and recursion |
| NoFrills | Info Tooltips | `utility.info_tooltips` | SkyBlock item data and tooltip hooks |
| NoFrills | Price Tooltips | `utility.price_tooltips` | Pricing API, recipes, cache and stale-data behavior |
| NoFrills | No Render | `render.suppression` | Multiple entity/particle/render hooks |
| NoFrills | Animation Fix | `render.animation_fix` | Version-specific animation hooks |
| NoFrills | Middle Click Fix | `utility.middle_click_fix` | Screen slot input semantics |
| NoFrills | Middle Click Override | `utility.middle_click_override` | Must arbitrate with middle-click fix |
| NoFrills | No Cursor Reset | `utility.cursor` | Mouse grabbing and screen transitions |
| NoFrills | No Loading Screen | `utility.loading_screen` | Do not bypass world readiness or disconnect screens |
| NoFrills | Experiment Solver | `solver.experiments` | Inventory content, menu identity, slot updates and reset |
| Stray | Skybox | `render.skybox` | Sky extraction, tint, textures and shader compatibility |
| Stray | Ambience | `render.ambience` | Lighting/time/weather interactions |
| Stray | Fog | `render.fog` | Optional Sodium-specific mixin must be guarded |
| Stray | Aspect | `render.aspect` | Projection and FOV behavior |
| Stray | Motion Blur | `render.motion_blur` | GPU resources, resize, temporal reset, Iris integration |
| Stray | Player Glow | `render.player_glow` | Player filtering and shared glow pass |
| Stray | Nametags | `render.nametags` | Render extraction and text layout |
| Stray | Block Outline | `render.block_outline` | Outline framebuffer and render-state restoration |
| Stray | Picture in Picture | `render.pip` | Actual feature captures another desktop window; Win32/JNA backend |
| Stray | Titanium ESP | `mining.titanium` | Block tracking, location context and world renderer |
| Stray | Skill Progress | `hud.skill_progress` | Chat/state tracking and persistence |
| Stray | Inventory | `hud.inventory` | Item renderer and HUD positions |
| Stray | Scoreboard | `hud.scoreboard` | Vanilla replacement and text layout |
| Stray | Item Name | `hud.item_name` | Confirm precise feature toggle semantics before porting |
| Stray | Themed Chat | `hud.themed_chat` | Chat layout, clipping, input and history |
| Stray | CS2 Knife | `cosmetic.cs2_knife` | Model/material/shader assets and their provenance need separate review |
| Stray | Held Item | `cosmetic.held_item` | Separate HUD/shader behavior from knife and item-name features |

## Pinned source entry points

### Odin — Dungeon Waypoints
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DialogComponents.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DialogComponents.kt)
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointEditor.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointEditor.kt)
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointHud.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointHud.kt)
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointPacks.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointPacks.kt)
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypoints.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypoints.kt)
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/SecretWaypoints.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/SecretWaypoints.kt)
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/TextPromtScreen.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/TextPromtScreen.kt)
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/WaypointPackSelectorScreen.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/WaypointPackSelectorScreen.kt)
### Odin — Ice Fill
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/puzzlesolvers/IceFillSolver.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/puzzlesolvers/IceFillSolver.kt)
### Odin — Blaze
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/puzzlesolvers/BlazeSolver.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/puzzlesolvers/BlazeSolver.kt)
### Odin — Creeper Beams
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/puzzlesolvers/BeamsSolver.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/puzzlesolvers/BeamsSolver.kt)
### Odin — Door Highlight
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/DoorHighlight.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/DoorHighlight.kt)
### Odin — Map Info
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/MapInfo.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/MapInfo.kt)
### Odin — Mimic detection/highlight
- [src/main/kotlin/com/odtheking/odin/features/impl/dungeon/Mimic.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/Mimic.kt)
### Odin — Tick Timers
- [src/main/kotlin/com/odtheking/odin/features/impl/boss/TickTimers.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/boss/TickTimers.kt)
### Odin — Player Display
- [src/main/kotlin/com/odtheking/odin/features/impl/skyblock/PlayerDisplay.kt](https://github.com/odtheking/Odin/blob/19118e15bfb57a953c3264373a32b42171c5f281/src/main/kotlin/com/odtheking/odin/features/impl/skyblock/PlayerDisplay.kt)
### NoFrills — Chat Tweaks
- [src/main/java/nofrills/features/general/ChatTweaks.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/general/ChatTweaks.java)
### NoFrills — Command Keybinds
- [src/main/java/nofrills/features/general/CommandKeybinds.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/general/CommandKeybinds.java)
### NoFrills — Command Shortcuts
- [src/main/java/nofrills/features/general/CommandShortcuts.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/general/CommandShortcuts.java)
### NoFrills — Info Tooltips
- [src/main/java/nofrills/features/general/InfoTooltips.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/general/InfoTooltips.java)
### NoFrills — Price Tooltips
- [src/main/java/nofrills/features/general/PriceTooltips.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/general/PriceTooltips.java)
### NoFrills — No Render
- [src/main/java/nofrills/features/general/NoRender.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/general/NoRender.java)
### NoFrills — Animation Fix
- [src/main/java/nofrills/features/tweaks/AnimationFix.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/tweaks/AnimationFix.java)
### NoFrills — Middle Click Fix
- [src/main/java/nofrills/features/tweaks/MiddleClickFix.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/tweaks/MiddleClickFix.java)
### NoFrills — Middle Click Override
- [src/main/java/nofrills/features/tweaks/MiddleClickOverride.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/tweaks/MiddleClickOverride.java)
### NoFrills — No Cursor Reset
- [src/main/java/nofrills/features/tweaks/NoCursorReset.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/tweaks/NoCursorReset.java)
### NoFrills — No Loading Screen
- [src/main/java/nofrills/features/tweaks/NoLoadingScreen.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/tweaks/NoLoadingScreen.java)
### NoFrills — Experiment Solver
- [src/main/java/nofrills/features/solvers/ExperimentSolver.java](https://github.com/WhatYouThing/NoFrills/blob/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a/src/main/java/nofrills/features/solvers/ExperimentSolver.java)
### Stray — Skybox
- [src/client/java/dev/stray/client/mixin/SkyRendererMixin.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/mixin/SkyRendererMixin.java)
- [src/client/java/dev/stray/client/visual/WorldTint.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/WorldTint.java)
### Stray — Ambience
- [src/client/java/dev/stray/client/visual/CustomAmbience.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/CustomAmbience.java)
### Stray — Fog
- [src/client/java/dev/stray/client/mixin/FogRendererMixin.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/mixin/FogRendererMixin.java)
- [src/client/java/dev/stray/client/mixin/sodium/SodiumChunkFogMixin.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/mixin/sodium/SodiumChunkFogMixin.java)
- [src/client/java/dev/stray/client/visual/CustomFog.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/CustomFog.java)
### Stray — Aspect
- [src/client/java/dev/stray/client/render/AspectFov.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/AspectFov.java)
### Stray — Motion Blur
- [src/client/java/dev/stray/client/visual/motionblur/BlurStrengthCalculator.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/BlurStrengthCalculator.java)
- [src/client/java/dev/stray/client/visual/motionblur/CameraState.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/CameraState.java)
- [src/client/java/dev/stray/client/visual/motionblur/FrameBlendingManager.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/FrameBlendingManager.java)
- [src/client/java/dev/stray/client/visual/motionblur/FrameTimer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/FrameTimer.java)
- [src/client/java/dev/stray/client/visual/motionblur/GpuBufferUtil.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/GpuBufferUtil.java)
- [src/client/java/dev/stray/client/visual/motionblur/ManagedUniformBuffer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/ManagedUniformBuffer.java)
- [src/client/java/dev/stray/client/visual/motionblur/MonitorInfoProvider.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/MonitorInfoProvider.java)
- [src/client/java/dev/stray/client/visual/motionblur/MotionBlurShaders.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/motionblur/MotionBlurShaders.java)
### Stray — Player Glow
- [src/client/java/dev/stray/client/render/MobGlowRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/MobGlowRenderer.java)
### Stray — Nametags
- [src/client/java/dev/stray/client/render/NametagRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/NametagRenderer.java)
### Stray — Block Outline
- [src/client/java/dev/stray/client/render/BlockOutlineGlow.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/BlockOutlineGlow.java)
### Stray — Picture in Picture
- [src/client/java/dev/stray/client/pip/PipCapture.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/pip/PipCapture.java)
- [src/client/java/dev/stray/client/pip/PipWin32.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/pip/PipWin32.java)
- [src/client/java/dev/stray/client/render/PipHudRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/PipHudRenderer.java)
### Stray — Titanium ESP
- [src/client/java/dev/stray/client/mining/TitaniumTracker.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/mining/TitaniumTracker.java)
### Stray — Skill Progress
- [src/client/java/dev/stray/client/render/SkillProgressHudRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/SkillProgressHudRenderer.java)
- [src/client/java/dev/stray/client/skill/SkillProgressTracker.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/skill/SkillProgressTracker.java)
### Stray — Inventory
- [src/client/java/dev/stray/client/render/InventoryHudRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/InventoryHudRenderer.java)
### Stray — Scoreboard
- [src/client/java/dev/stray/client/render/ScoreboardHudRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/ScoreboardHudRenderer.java)
### Stray — Item Name
- [src/client/java/dev/stray/client/render/HeldItemHudRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/HeldItemHudRenderer.java)
- [src/client/java/dev/stray/client/render/VanillaHud.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/VanillaHud.java)
### Stray — Themed Chat
- [src/client/java/dev/stray/client/mixin/ChatComponentAccessor.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/mixin/ChatComponentAccessor.java)
- [src/client/java/dev/stray/client/mixin/ChatComponentFillMixin.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/mixin/ChatComponentFillMixin.java)
- [src/client/java/dev/stray/client/mixin/ChatComponentMixin.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/mixin/ChatComponentMixin.java)
- [src/client/java/dev/stray/client/ui/ChatChrome.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/ui/ChatChrome.java)
### Stray — CS2 Knife
- [src/client/java/dev/stray/client/cs2/Cs2Knives.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/Cs2Knives.java)
- [src/client/java/dev/stray/client/cs2/Cs2Library.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/Cs2Library.java)
- [src/client/java/dev/stray/client/cs2/Cs2Viewmodel.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/Cs2Viewmodel.java)
- [src/client/java/dev/stray/client/cs2/GripDefaults.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/GripDefaults.java)
- [src/client/java/dev/stray/client/cs2/anim/NmClip.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/anim/NmClip.java)
- [src/client/java/dev/stray/client/cs2/anim/NmSkeleton.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/anim/NmSkeleton.java)
- [src/client/java/dev/stray/client/cs2/anim/Pose.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/anim/Pose.java)
- [src/client/java/dev/stray/client/cs2/pack/Cs2Pack.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/pack/Cs2Pack.java)
- [src/client/java/dev/stray/client/cs2/pack/Cs2PackBuilder.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/pack/Cs2PackBuilder.java)
- [src/client/java/dev/stray/client/cs2/render/KnifeGpu.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/render/KnifeGpu.java)
- [src/client/java/dev/stray/client/cs2/render/KnifeRender.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/render/KnifeRender.java)
- [src/client/java/dev/stray/client/cs2/render/SoftwareRender.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/render/SoftwareRender.java)
- [src/client/java/dev/stray/client/cs2/s2/Bcn.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Bcn.java)
- [src/client/java/dev/stray/client/cs2/s2/Kv1.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Kv1.java)
- [src/client/java/dev/stray/client/cs2/s2/Kv3.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Kv3.java)
- [src/client/java/dev/stray/client/cs2/s2/Lz4.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Lz4.java)
- [src/client/java/dev/stray/client/cs2/s2/Meshopt.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Meshopt.java)
- [src/client/java/dev/stray/client/cs2/s2/Resource.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Resource.java)
- [src/client/java/dev/stray/client/cs2/s2/S2Model.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/S2Model.java)
- [src/client/java/dev/stray/client/cs2/s2/Vpk.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Vpk.java)
- [src/client/java/dev/stray/client/cs2/s2/Vtex.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Vtex.java)
- [src/client/java/dev/stray/client/cs2/s2/Zstd.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/Zstd.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/BitInputStream.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/BitInputStream.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/BitOutputStream.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/BitOutputStream.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/Constants.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/Constants.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/FiniteStateEntropy.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/FiniteStateEntropy.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/FrameHeader.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/FrameHeader.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/FseCompressionTable.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/FseCompressionTable.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/FseTableReader.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/FseTableReader.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/Histogram.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/Histogram.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/Huffman.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/Huffman.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/MalformedInputException.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/MalformedInputException.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/UnsafeUtil.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/UnsafeUtil.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/Util.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/Util.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/XxHash64.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/XxHash64.java)
- [src/client/java/dev/stray/client/cs2/s2/zstd/ZstdFrameDecompressor.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/s2/zstd/ZstdFrameDecompressor.java)
- [src/client/java/dev/stray/client/cs2/skin/Assets.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/skin/Assets.java)
- [src/client/java/dev/stray/client/cs2/skin/Composer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/skin/Composer.java)
- [src/client/java/dev/stray/client/cs2/skin/KnifeMaterial.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/skin/KnifeMaterial.java)
- [src/client/java/dev/stray/client/cs2/skin/PaintKit.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/skin/PaintKit.java)
- [src/client/java/dev/stray/client/cs2/skin/Tex.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/skin/Tex.java)
- [src/client/java/dev/stray/client/cs2/skin/ValveRandom.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/cs2/skin/ValveRandom.java)
### Stray — Held Item
- [src/client/java/dev/stray/client/render/HeldItemHudRenderer.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/render/HeldItemHudRenderer.java)
- [src/client/java/dev/stray/client/visual/HeldItemShader.java](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/src/client/java/dev/stray/client/visual/HeldItemShader.java)

## impl.zip: every feature object

The archive contains **34 Kotlin files**, including **23 detected Feature objects**. Helpers are tracked in `audit/archive-inventory.json`; none are dropped.

| Feature source | Required integration |
|---|---|
| `impl/dungeon/MimicChest.kt` | Chest/entity evidence, world overlays; reconcile with Odin Mimic without losing options. |
| `impl/dungeon/OdinDungeonWaypoints.kt` | Room-local transforms, Odin codec, route steps, editor input, secret events and persistence; reconcile both waypoint implementations. |
| `impl/dungeon/SecretCounter.kt` | Chat/dungeon run events and state persistence. |
| `impl/dungeon/ShitterList.kt` | Local list/commands, Mojang profile lookups; retain MIT-derived attribution; use bounded background I/O. |
| `impl/dungeon/SimonSays.kt` | Packet/block observations, button state, render hints; inspect interaction modes and cancellation. |
| `impl/general/AetherOSSettings.kt` | Shared theme palette and settings; preserve styling options behind future UI adapter. |
| `impl/general/BestiaryESP.kt` | Name metadata, armor-stand-to-mob association, entity unload, glow/tracers. |
| `impl/general/PetDisplay.kt` | Container, chat and packet events; inspect external item data and HUD. |
| `impl/general/Timer.kt` | Keyboard/mouse inputs, monotonic timer and HUD rendering. |
| `impl/hunting/InvisibugESP.kt` | Entity metadata packets, tick tracking and rendering. |
| `impl/hunting/TikiESP.kt` | Tower recognition, loaded block/entity state and overlays. |
| `impl/qol/AutoRag.kt` | Automated casting, slot swapping and optional pet selection; explicit opt-in and server-rule warning. |
| `impl/qol/AutoReel.kt` | Automated use-item input on REEL labels; opt-in, cooldown, pending-action cancellation on world changes. |
| `impl/visual/AlwaysWater.kt` | Feature is a hook-facing toggle; external consumer/mixin absent from archive. |
| `impl/visual/AmbientParticles.kt` | Particle settings/hook-facing feature; external consumer must be recovered. |
| `impl/visual/ChinaHat.kt` | World geometry and player pose. |
| `impl/visual/Cosmetics.kt` | Render-state hooks, badge metadata, halo/wings models, text replacement; external services/assets need review. |
| `impl/visual/DvdBounce.kt` | HUD text, bounds and resize behavior. |
| `impl/visual/Halo.kt` | World geometry, blend/depth state and player positioning. |
| `impl/visual/ItemAura.kt` | Item rendering, capture buffers, GUI/font and GPU integration. |
| `impl/visual/MoveHearts.kt` | HUD editor and vanilla hearts hooks absent from archive. |
| `impl/visual/MoveHotbar.kt` | Custom hotbar rendering, layout/editor, status indicators and config. |
| `impl/visual/RotatingItem.kt` | Held-item transform hook consumer must be recovered. |

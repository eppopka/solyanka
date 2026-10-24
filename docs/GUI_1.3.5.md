# 1.3.5 — startup black-screen hotfix

The 1.3.4 Held Item pipeline combined ITEM_SNIPPET and GLOBALS_SNIPPET. Native 26.2 ITEM_SNIPPET already inherits Globals through MATRICES_FOG_LIGHT_DIR_SNIPPET and MATRICES_FOG_SNIPPET. The extra snippet duplicated the binding; Minecraft rejected the required startup pipeline, retried resource loading, and could not show the menu. This happens even with the module disabled because pipeline registration is global.

Removed the redundant GLOBALS_SNIPPET. Existing settings and all other features are unchanged.

Regression coverage uses Minecraft 26.2 BindGroupLayout.ensureCompatible on actual native snippets: the previous layout reproduces the reported duplicate Globals error; the corrected six-group layout is accepted. This is native metadata validation, not a client launch or GPU compile. A source regression also prevents reintroducing the extra snippet.

Install: close Minecraft, replace the old solyanka JAR with 1.3.5 (do not keep both). No configuration reset required. Verify the loading screen reaches the main menu, then test Held Item in a world. Send latest.log if another error remains.

Raw user log is not included in this source package.

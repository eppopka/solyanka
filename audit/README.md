# Current audit status — 0.5.0-dev

The feature JSON now distinguishes adapted implementations from unported entries. Archive MimicChest is implemented separately; all other archive objects remain pending. docs/M4.md traces new source paths, bundled resources and deviations. Source-lock.json pins repositories; license texts accompany derivatives. No repo-wide completion/compatibility claim.

## Initial audit notes (historical)

# Audit records

These files are evidence, not implementation source or dependencies. `source-lock.json` pins repository commits and the original attachment hash. `features.json` maps requested names to entry-point source files. `archive-inventory.json` inventories all attached Kotlin files and their imports/event registrations. `licenses/` stores repository-level license text only. `verified-*.txt` retains official version/toolchain data used for this milestone.

Not every transitive dependency or embedded asset has been reviewed. See ../docs/AUDIT.md for limits and distribution gates.

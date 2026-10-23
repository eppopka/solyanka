# Audit records

These files are evidence, not implementation source or dependencies. `source-lock.json` pins repository commits and the original attachment hash. `features.json` maps requested names to entry-point source files. `archive-inventory.json` inventories all attached Kotlin files and their imports/event registrations. `licenses/` stores repository-level license text only. `verified-*.txt` retains official version/toolchain data used for this milestone.

Not every transitive dependency or embedded asset has been reviewed. See ../docs/AUDIT.md for limits and distribution gates.

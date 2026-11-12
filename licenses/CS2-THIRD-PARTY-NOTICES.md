# Third-party code in the CS2 knives feature

Parts of this package are ported from or based on the projects below.

| Project | Used for | License |
| --- | --- | --- |
| [ValveResourceFormat](https://github.com/ValveResourceFormat/ValveResourceFormat) | VPK, resource blocks, KV3, vtex, models, NmClip/NmSkeleton decoding | MIT |
| [meshoptimizer](https://github.com/zeux/meshoptimizer) | vertex and index buffer decoding (`thirdparty/meshopt/Meshopt.java`); the pinned Stray source calls this a port from ValveResourceFormat | MIT; ValveResourceFormat's [third-party notice](https://github.com/ValveResourceFormat/ValveResourceFormat/blob/master/THIRD_PARTY_NOTICES.txt) also lists meshoptimizer under MIT |
| [DirectXTex](https://github.com/microsoft/DirectXTex) | BC7 partition and index tables (`s2/Bcn.java`), as recorded by this notice | MIT upstream; the audited `Bcn.java` has no DirectXTex source pointer or header, so this specific provenance is not independently verified |
| [aircompressor](https://github.com/airlift/aircompressor) | Zstandard decoder (`thirdparty/zstd/`) | Apache 2.0; most decoder files retain Apache headers |

## Audit evidence and unresolved provenance

[`audit/final-cosmetics-source-manifest.json`](../audit/final-cosmetics-source-manifest.json) pins the reviewed source snapshot to Stray commit [`d91fcd4515b4eda647ee8fd7fa99408f473e5718`](https://github.com/camberX/Stray/tree/d91fcd4515b4eda647ee8fd7fa99408f473e5718) and records hashes for the copied files. Stray's [license at that revision](https://github.com/camberX/Stray/blob/d91fcd4515b4eda647ee8fd7fa99408f473e5718/LICENSE) is CC0-1.0. The manifest's existing `CC0-1.0 or retained third-party headers` label is not a per-file provenance determination, and it does not record upstream revisions for the third-party-derived code.

- `thirdparty/lz4/Lz4.java` (and the audited Stray copy) has no license header or named upstream source. The repository evidence does not establish whether it is original CC0 code or a third-party derivative; its source and license remain unresolved. No upstream project is inferred here.
- `s2/Bcn.java` (and its audited Stray copy) has no DirectXTex header or source reference. DirectXTex is MIT-licensed upstream ([license](https://github.com/microsoft/DirectXTex/blob/main/LICENSE)), but the existing BC7-table attribution has no retained source path, revision, or mapping to those tables, so the specific derivation remains unresolved.
- The audited Stray `s2/Meshopt.java` comment identifies meshoptimizer vertex/index decoders ported from ValveResourceFormat. ValveResourceFormat is MIT-licensed ([license](https://github.com/ValveResourceFormat/ValveResourceFormat/blob/master/LICENSE)); its third-party notice lists meshoptimizer as MIT, consistent with meshoptimizer's [license](https://github.com/zeux/meshoptimizer/blob/master/LICENSE.md). The audit pins the Stray copy, not the upstream source revision or a line-level mapping.
- The Zstandard source comments identify aircompressor; its project metadata declares Apache License 2.0 ([metadata](https://github.com/airlift/aircompressor/blob/master/pom.xml)), and the audited decoder files retain Apache headers. `thirdparty/zstd/MalformedInputException.java` and `thirdparty/zstd/UnsafeUtil.java` are marked as local adaptations but have no upstream Apache headers or upstream source mapping in the audit. Whether those helpers are original adaptations or derivatives, and their precise provenance, remains unresolved.

The license text below preserves the MIT attributions above; it does not resolve the outstanding source mappings.

The Counter-Strike 2 knife data (models, textures, animations) is not part of this repository's
code; it is published separately on the `cs2-data` branch with permission from Valve, for
non-commercial use.

## MIT License (ValveResourceFormat, meshoptimizer, DirectXTex)

Copyright (c) 2015 ValveResourceFormat Contributors;
Copyright (c) 2016-2026 Arseny Kapoulkine;
Copyright (c) Microsoft Corporation.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

# Attribution and modification notice

This modified version is published by SeasideH1 on 2026-10-09 as TaCZ-vulkan.
It derives from MCModderAnchor/TACZ 1.1.8-hotfix2, commit
`b482eff8c94a733ac8d0910193fca3893954027c`:
https://github.com/MCModderAnchor/TACZ/tree/b482eff8c94a733ac8d0910193fca3893954027c

Original programmer credits: 286799714, TartaricAcid, F1zeiL, xjqsh, ClumsyAlien.
Original artist credits: NekoCrane, Receke, Pos_2333.
Original credits and license statements are retained in individual files and
[the pinned upstream README](forge-release-port/upstream-reference/TACZ-README.md).
No affiliation with or endorsement by the upstream authors is implied.

Changes include Forge-to-Fabric platform adaptation; target-version registry,
data, recipe, networking and rendering API migration; native gun and scope
rendering; gunpack compatibility fixes; muzzle pose snapshots; packed geometry,
bounded GPU buffer caching, adjacent compatible draw batching and small-detail LOD.
Unsupported integration source modules are separated from the active build.
Build/reproduction scripts and dependency notices have been added. The publication
also normalizes Windows extended-length paths before the downloader containment
check, retaining link resolution and rejecting paths outside the checkout.

The current implementation is based on candidate18. Publication adds modification
headers to 506 added or changed active Java files, plus public documentation and public-facing Fabric / integration metadata.
The exact pre-notice Java hashes and changed-file list are recorded in
`reproduction/source-provenance.json`. This public tree is not claimed to be a
byte-identical reproduction of the previously built candidate18 JAR.

All existing files under upstream `src/main/resources/assets/` that are present
here were compared with Git blob hashes from the pinned upstream commit: their
bytes are unchanged. New target-version item definitions and shader program code
are additions; they do not relicense the original artwork.

The three SimpleBedrockModel 2.2.2 adaptations preserve LGPL-3.0 notices and
original authors TartaricAcid, MaydayMemory, MoePus, Hidomatn and xjqsh.
Original sources and corresponding source archive are included. Dependency
licenses, notices, official URLs and hashes are retained under
`forge-release-port/src/main/resources/META-INF/` and `reproduction/`.

TACZ program code and this port's original program/build code are distributed
under GNU GPL version 3, subject to preserved upstream notices and the separate
third-party licenses identified above. Original artwork is separately licensed;
see ASSET_LICENSE.md. There is no warranty; see the applicable licenses.

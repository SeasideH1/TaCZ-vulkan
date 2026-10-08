#!/usr/bin/env python3
"""Real recipe/native-inventory checks with pinned full-candidate dependency closure.

This is a leaf harness, not a fabricated Player/ServerLevel or game-runtime test.
It uses a documented dedicated-process bootstrap guard for pre-freeze registration.
"""
import datetime as dt
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SHARED = ROOT.parent
JAVA = SHARED / "toolchains/jdk-25.0.4.1+1/bin"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    out = ROOT / "build/crafting-checks/runs" / stamp
    classes = out / "classes"
    classes.mkdir(parents=True)
    manifest = ROOT / "build/direct/runtime-candidate-latest.json"
    candidate = json.loads(manifest.read_text())
    jar = Path(candidate["jar"])
    assert sha(jar) == candidate["sha256"], "Candidate dependency closure drift"
    classpath = [str(jar)]
    for folder in ("official-minecraft", "official-java-libraries", "official-fabric"):
        classpath += (SHARED / folder / "classpath.txt").read_text().strip().split(":")
    sources = [ROOT / ("src/main/java/com/tacz/guns/" + p + ".java") for p in (
        "crafting/GunSmithTableCrafting", "inventory/GunSmithTableMenu")]
    sources += [ROOT / "src/test/java/com/tacz/guns/crafting/AllRecipeCraftingChecks.java"]
    resources = list((ROOT / "src/main/resources/assets/tacz/custom/tacz_default_gun/data").rglob("*.json"))
    resources += [ROOT / "src/main/resources/data/tacz/painting_variant/blood_strike_1.json",
                  ROOT / "src/main/resources/data/tacz/recipes/misc/blood_strike_1.json"]
    before = {str(p): sha(p) for p in sources + resources}
    dependencies = {p: sha(Path(p)) for p in classpath}
    compile_cmd = [str(JAVA / "javac"), "-proc:none", "--release", "25", "-cp", ":".join(classpath),
                   "-d", str(classes), *map(str, sources)]
    with (out / "compile.log").open("w") as log:
        compile_result = subprocess.run(compile_cmd, stdout=log, stderr=subprocess.STDOUT)
    test_exit = None
    if compile_result.returncode == 0:
        command = [str(JAVA / "java"), "-cp", str(classes) + ":" + ":".join(classpath),
                   "com.tacz.guns.crafting.AllRecipeCraftingChecks"]
        with (out / "result.log").open("w") as log:
            try:
                test_exit = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=180).returncode
            except subprocess.TimeoutExpired:
                test_exit = 124
    after = {str(p): sha(p) for p in sources + resources}
    status = "PASS" if compile_result.returncode == test_exit == 0 and before == after else "FAIL"
    report = {"kind": "ACTUAL_RECIPE_NATIVE_INVENTORY_LEAF_CHECKS", "status": status, "timestamp": stamp,
              "compile_exit": compile_result.returncode, "test_exit": test_exit, "target": "26.4-snapshot-3",
              "candidate_dependency_closure": candidate, "dependencies_sha256": dependencies,
              "source_and_resource_sha256_before": before, "source_and_resource_sha256_after": after,
              "stable": before == after, "runner_sha256": sha(Path(__file__)),
              "bootstrap": "TEST_ONLY pre-freeze guard bridge; actual native vanilla component initialization",
              "tags": "Actual vanilla resources and published Fabric convention tags loaded by native TagLoader",
              "runtime_menu_packet_entity_spawn": "NOT_RUN", "production_full_source_compile": "SEPARATE_GATE",
              "baseline_limitation": "Overlapping ingredient predicates can reuse available counts; characterized and retained. No overlap in the 173 official recipes under the exact target tags.",
              "logs": [str(out / "compile.log"), str(out / "result.log")]}
    (out / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    (ROOT / "build/crafting-checks/report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(status, out / "report.json")
    print((out / "result.log").read_text() if (out / "result.log").exists() else (out / "compile.log").read_text())
    return 0 if status == "PASS" else 1

if __name__ == "__main__":
    raise SystemExit(main())

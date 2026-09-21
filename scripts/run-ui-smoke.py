"""Run the UI test without Gradle's post-test uninstall, preserving PNG evidence."""
import pathlib
import re
import subprocess
import sys

result = subprocess.run(
    ["adb", "shell", "am", "instrument", "-w", "-r", "-e", "class",
     "ir.kamranvahdati.lawoffice.UiFlowSmokeTest",
     "ir.kamranvahdati.lawoffice.test/androidx.test.runner.AndroidJUnitRunner"],
    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=240,
)
path = pathlib.Path("app/build/ui-smoke.txt")
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(result.stdout, encoding="utf-8")
print(result.stdout, flush=True)
# adb itself can exit zero even when Android instrumentation reports a failure.
passed = (result.returncode == 0
          and re.search(r"OK\s*\(1 test\)", result.stdout)
          and "INSTRUMENTATION_CODE: -1" in result.stdout
          and "FAILURES!!!" not in result.stdout)
sys.exit(0 if passed else 1)

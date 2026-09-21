"""Run the UI test without Gradle's post-test uninstall, preserving PNG evidence."""
import pathlib
import io
import re
import subprocess
import sys
import tarfile

result = subprocess.run(
    ["adb", "shell", "am", "instrument", "-w", "-r", "-e", "class",
     "ir.kamranvahdati.lawoffice.UiFlowSmokeTest",
     "ir.kamranvahdati.lawoffice.test/ir.kamranvahdati.lawoffice.OfficeTestRunner"],
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
if not passed:
    sys.exit(1)

# Android scoped storage denies the shell direct access to another UID's files.
# run-as is available only for this debuggable test build; production permissions stay intact.
archive = subprocess.run(
    ["adb", "exec-out", "run-as", "ir.kamranvahdati.lawoffice", "tar", "-C",
     "/data/user/0/ir.kamranvahdati.lawoffice/files/qa", "-cf", "-", "."],
    stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=60, check=True,
)
destination = pathlib.Path("app/build/qa")
destination.mkdir(parents=True, exist_ok=True)
images = 0
with tarfile.open(fileobj=io.BytesIO(archive.stdout)) as screenshots:
    for member in screenshots.getmembers():
        if not member.isfile():
            continue
        name = pathlib.PurePosixPath(member.name)
        if name.suffix != ".png" or ".." in name.parts or member.size > 10_000_000:
            raise RuntimeError("Unexpected screenshot archive entry")
        (destination / name.name).write_bytes(screenshots.extractfile(member).read())
        images += 1
if images < 10:
    raise RuntimeError(f"Expected dashboard/settings for five themes, found {images}")
print(f"Collected {images} UI screenshots", flush=True)

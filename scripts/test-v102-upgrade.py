"""Installed same-package test-signed V10 or V10.1 -> V10.2, without data clearing."""
import os
import pathlib
import subprocess

baseline = pathlib.Path(os.environ['VOKANO_BASELINE_APK'])
baseline_test = pathlib.Path(os.environ['VOKANO_BASELINE_TEST_APK'])
candidate = pathlib.Path('app/build/outputs/apk/release/app-release.apk')
candidate_test = pathlib.Path('app/build/outputs/apk/androidTest/release/app-release-androidTest.apk')
out = pathlib.Path('app/build/v102-proof') / os.environ['VOKANO_BASELINE_NAME']
out.mkdir(parents=True, exist_ok=True)
package = 'ir.kamranvahdati.lawoffice'

def command(*args):
    result = subprocess.run(args, text=True, capture_output=True, timeout=300)
    output = result.stdout + result.stderr
    if result.returncode:
        raise RuntimeError(f'{args[0]} {args[1:3]} failed ({result.returncode}): {output}')
    return output

def adb(*args):
    return command('adb', *args)

def test(name, phase=None):
    args = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'class', package + '.' + name]
    if phase:
        args += ['-e', 'upgrade_phase', phase]
    result = adb(*args, package + '.test/' + package + '.OfficeTestRunner')
    (out / (name.replace('#', '-') + '-' + (phase or 'suite') + '.txt')).write_text(result)
    print(result, flush=True)
    assert 'OK (' in result and 'FAILURES' not in result and 'INSTRUMENTATION_FAILED' not in result

apksigner = pathlib.Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0/apksigner'
certs = []
for apk in (baseline, candidate):
    certs.append(command(str(apksigner), 'verify', '--print-certs', str(apk)))
assert certs[0] == certs[1], 'APK signing identities differ'
(out / 'test-signing-certificates.txt').write_text(certs[0])
assert baseline != candidate

adb('install', str(baseline))
adb('install', str(baseline_test))
test('V102UpgradeTest', 'seed')
before = adb('shell', 'dumpsys', 'package', package)
(out / 'before-package.txt').write_text(before)
adb('shell', 'am', 'force-stop', package)
adb('install', '-r', str(candidate))
adb('install', '-r', str(candidate_test))
test('V102UpgradeTest', 'verify')
for scoped in ['V102StorageTest', 'WorkspaceProviderTest', 'ThemeAndProfileAssetsTest', 'UiFlowSmokeTest']:
    test(scoped)
after = adb('shell', 'dumpsys', 'package', package)
(out / 'after-package.txt').write_text(after)
assert 'versionCode=15' in after
try:
    adb('root')
    adb('wait-for-device')
    adb('pull', f'/sdcard/Android/data/{package}/files/qa', str(out / 'screenshots'))
except RuntimeError as exc:
    (out / 'screenshot-pull-error.txt').write_text(str(exc))

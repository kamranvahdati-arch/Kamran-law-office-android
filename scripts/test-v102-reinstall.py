"""Destructive lifecycle test restricted to an otherwise empty CI emulator.

The separate test APK owns synthetic documents and deliberately remains installed.
Never run this against an owner's phone or an emulator containing office data.
"""
import pathlib
import subprocess

PACKAGE = 'ir.kamranvahdati.lawoffice'
OUT = pathlib.Path('app/build/v102-proof/clean-reinstall')
OUT.mkdir(parents=True, exist_ok=True)

def adb(*args):
    result = subprocess.run(['adb', *args], capture_output=True, text=True, timeout=300)
    output = result.stdout + result.stderr
    if result.returncode:
        raise RuntimeError(output)
    return output

def test(name, phase=None):
    args = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'class', PACKAGE + '.' + name]
    if phase:
        args += ['-e', 'upgrade_phase', phase]
    result = adb(*args, PACKAGE + '.test/' + PACKAGE + '.OfficeTestRunner')
    (OUT / (name + '-' + (phase or 'suite') + '.txt')).write_text(result)
    print(result, flush=True)
    if 'OK (1 test)' not in result or 'FAILURES' in result or 'INSTRUMENTATION_FAILED' in result:
        raise RuntimeError('Instrumentation did not pass exactly one lifecycle test')

if adb('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
    raise RuntimeError('Only an isolated emulator may run this lifecycle test')
if ('package:' + PACKAGE) in adb('shell', 'pm', 'list', 'packages').splitlines():
    raise RuntimeError('Refusing lifecycle test: target was already installed')
candidate = 'app/build/outputs/apk/release/app-release.apk'
adb('install', candidate)
adb('install', 'app/build/outputs/apk/androidTest/release/app-release-androidTest.apk')
test('V102CleanInstallTest')
test('WorkspaceReinstallTest', 'seed')
# Only this synthetic, freshly installed target is removed, never the provider APK.
adb('uninstall', PACKAGE)
if ('package:' + PACKAGE + '.test') not in adb('shell', 'pm', 'list', 'packages').splitlines():
    raise RuntimeError('External workspace provider unexpectedly removed')
adb('install', candidate)
test('WorkspaceReinstallTest', 'verify')

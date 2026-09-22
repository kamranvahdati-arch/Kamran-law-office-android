"""Test the installed release APK, including backup across an actual pm clear."""
import pathlib, re, subprocess
PACKAGE = 'ir.kamranvahdati.lawoffice'
ROOT = pathlib.Path('app/build/production-proof')
ROOT.mkdir(parents=True, exist_ok=True)

def adb(*args, **kwargs):
    return subprocess.run(['adb', *args], check=True, timeout=600, **kwargs)

def host_files(*commands):
    # API 30 blocks the shell UID from Android/data. Elevate only the emulator
    # transport for host-side escrow; the release app and instrumentation retain
    # their normal application UID and run after adbd returns to shell UID.
    adb('root')
    adb('wait-for-device')
    try:
        assert adb('shell', 'id', '-u', text=True, stdout=subprocess.PIPE).stdout.strip() == '0'
        for command in commands:
            adb(*command)
    finally:
        adb('unroot')
        adb('wait-for-device')
    assert adb('shell', 'id', '-u', text=True, stdout=subprocess.PIPE).stdout.strip() == '2000'

def test(name, classes, phase=None):
    # Each invocation starts a fresh process; finished Activity instances and
    # pending window transitions from other test classes must not be reused.
    adb('shell', 'am', 'force-stop', PACKAGE)
    args = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'class', classes]
    if phase:
        args += ['-e', 'production_mode', phase]
    args += [PACKAGE + '.test/' + PACKAGE + '.OfficeTestRunner']
    result = adb(*args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (ROOT / (name + '.txt')).write_text(result.stdout)
    print(result.stdout, flush=True)
    if not re.search(r'OK\s*\(\d+ tests?\)', result.stdout) or 'FAILURES!!!' in result.stdout or 'INSTRUMENTATION_CODE: -1' not in result.stdout:
        raise RuntimeError(name + ' failed')

adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/' + PACKAGE + '.MainActivity')
adb('shell', 'pidof', PACKAGE)
adb('shell', 'am', 'force-stop', PACKAGE)
test('01-empty-release-and-backup', PACKAGE + '.ProductionBaselineTest', 'seed')
external = '/sdcard/Android/data/' + PACKAGE + '/files/production-proof'
host_files(('pull', external + '/full.klo', str(ROOT / 'full.klo')),
           ('pull', external + '/expected.json', str(ROOT / 'expected.json')))
adb('shell', 'pm', 'clear', PACKAGE)
# Both private DB/key/media and app external storage were deleted by the OS.
host_files(('shell', 'mkdir', '-p', external),
           ('push', str(ROOT / 'full.klo'), external + '/full.klo'),
           ('push', str(ROOT / 'expected.json'), external + '/expected.json'))
test('02-restore-after-reset', PACKAGE + '.ProductionBaselineTest', 'restore')
adb('shell', 'wm', 'size', '720x1280')
for index, name in enumerate(['VokanoDashboardTest','UiContractTest','UiFlowSmokeTest','ThemeAndProfileAssetsTest'], 3):
    test('%02d-%s' % (index, name), PACKAGE + '.' + name)
adb('shell', 'wm', 'size', 'reset')
host_files(('pull', '/sdcard/Android/data/' + PACKAGE + '/files/qa', str(ROOT / 'screenshots')))
# Do not package synthetic backup/media as user data in release deliverables.
(ROOT / 'full.klo').unlink()
(ROOT / 'expected.json').unlink()
print('Release clean start, OS data reset, DB/media integrity, PDF/image opening and UI smoke PASS', flush=True)

"""Test the installed release APK, including backup across an actual pm clear."""
import pathlib, re, subprocess
PACKAGE = 'ir.kamranvahdati.lawoffice'
ROOT = pathlib.Path('app/build/production-proof')
ROOT.mkdir(parents=True, exist_ok=True)

def adb(*args, **kwargs):
    return subprocess.run(['adb', *args], check=True, timeout=600, **kwargs)

def test(name, classes, phase=None):
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
adb('pull', external + '/full.klo', str(ROOT / 'full.klo'))
adb('pull', external + '/expected.json', str(ROOT / 'expected.json'))
adb('shell', 'pm', 'clear', PACKAGE)
# Both private DB/key/media and app external storage were deleted by the OS.
adb('shell', 'mkdir', '-p', external)
adb('push', str(ROOT / 'full.klo'), external + '/full.klo')
adb('push', str(ROOT / 'expected.json'), external + '/expected.json')
test('02-restore-after-reset', PACKAGE + '.ProductionBaselineTest', 'restore')
adb('shell', 'wm', 'size', '720x1280')
test('03-release-dashboard-navigation-themes', ','.join(PACKAGE + '.' + x for x in ['VokanoDashboardTest','UiContractTest','UiFlowSmokeTest','ThemeAndProfileAssetsTest']))
adb('shell', 'wm', 'size', 'reset')
adb('pull', '/sdcard/Android/data/' + PACKAGE + '/files/qa', str(ROOT / 'screenshots'))
# Do not package synthetic backup/media as user data in release deliverables.
(ROOT / 'full.klo').unlink()
(ROOT / 'expected.json').unlink()
print('Release clean start, OS data reset, DB/media integrity, PDF/image opening and UI smoke PASS', flush=True)

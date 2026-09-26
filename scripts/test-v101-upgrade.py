"""Same-key test APK upgrade. This does not assert permanent production signing."""
import pathlib, subprocess
out=pathlib.Path('app/build/v101-proof');out.mkdir(parents=True,exist_ok=True)
def adb(*args):
    return subprocess.check_output(['adb',*args],text=True,stderr=subprocess.STDOUT)
def test(name,phase=None):
    args=['shell','am','instrument','-w','-r','-e','class',f'ir.kamranvahdati.lawoffice.{name}']
    if phase:args+=['-e','upgrade_phase',phase]
    result=adb(*args,'ir.kamranvahdati.lawoffice.test/ir.kamranvahdati.lawoffice.OfficeTestRunner')
    (out/f'{name}-{phase or "suite"}.txt').write_text(result)
    print(result)
    assert 'OK (' in result and 'FAILURES' not in result and 'INSTRUMENTATION_FAILED' not in result
base='/tmp/vokano-baseline/app/build/outputs/apk/'
adb('install',base+'release/app-release.apk')
adb('install',base+'androidTest/release/app-release-androidTest.apk')
test('V101UpgradeTest','seed')
adb('install','-r','app/build/outputs/apk/release/app-release.apk')
adb('install','-r','app/build/outputs/apk/androidTest/release/app-release-androidTest.apk')
test('V101UpgradeTest','verify')
test('V101DomainTest')

adb('root')
adb('wait-for-device')
adb('pull','/sdcard/Android/data/ir.kamranvahdati.lawoffice/files/v101-proof',str(out/'screenshots'))

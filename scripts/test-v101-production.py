"""Original permanent APK -> same-key 10.1 upgrade, with no uninstall/data clear."""
import pathlib,subprocess,json,hashlib,urllib.request,base64,os
root=pathlib.Path('app/build/permanent-proof');root.mkdir(parents=True,exist_ok=True)
manifest=json.loads(pathlib.Path('release-validation/v101.json').read_text())
repo='kamranvahdati-arch/Kamran-law-office-android'
for item in manifest['files']:
    path=pathlib.Path('candidate',item['name']);path.parent.mkdir(exist_ok=True)
    with path.open('wb') as out:
        for sha in item['blobs']:
            request=urllib.request.Request(f'https://api.github.com/repos/{repo}/git/blobs/{sha}',headers={'Authorization':'Bearer '+os.environ['GITHUB_TOKEN'],'Accept':'application/vnd.github+json'})
            with urllib.request.urlopen(request,timeout=60) as response: data=json.load(response)
            out.write(base64.b64decode(data['content']))
    assert hashlib.sha256(path.read_bytes()).hexdigest()==item['sha256']
apksigner=pathlib.Path(os.environ['ANDROID_HOME'],'build-tools/35.0.0/apksigner')
for path in ['original/VOKANO-10-release.apk','original/instrumentation.apk','candidate/VOKANO-10.1-release.apk','candidate/instrumentation.apk']:
    result=subprocess.check_output([str(apksigner),'verify','--print-certs',path],text=True)
    assert '26055f09370416e9cd61c08246b80c57367470cdb6873e282b5b6521cf097202' in result
    (root/(path.replace('/','-')+'.certificate.txt')).write_text(result)
def adb(*args):return subprocess.check_output(['adb',*args],text=True,stderr=subprocess.STDOUT,timeout=240)
def test(name,phase=None):
    args=['shell','am','instrument','-w','-r','-e','class','ir.kamranvahdati.lawoffice.'+name]
    if phase:args+=['-e','production_mode',phase]
    result=adb(*args,'ir.kamranvahdati.lawoffice.test/ir.kamranvahdati.lawoffice.OfficeTestRunner')
    (root/(name+'-'+(phase or 'verify')+'.txt')).write_text(result);print(result)
    assert 'OK (' in result and 'FAILURES' not in result and 'INSTRUMENTATION_FAILED' not in result
adb('install','original/VOKANO-10-release.apk')
adb('install','original/instrumentation.apk')
test('ProductionBaselineTest#emptyReleaseAndCompleteBackupAcrossApplicationDataReset','seed')
adb('shell','am','force-stop','ir.kamranvahdati.lawoffice')
adb('install','-r','candidate/VOKANO-10.1-release.apk')
adb('install','-r','candidate/instrumentation.apk')
test('V101ProductionUpgradeTest')
test('V101DomainTest')
adb('root');adb('wait-for-device')
adb('pull','/sdcard/Android/data/ir.kamranvahdati.lawoffice/files/v101-proof',str(root/'screenshots'))
(root/'provenance.json').write_text(json.dumps(manifest,indent=2))

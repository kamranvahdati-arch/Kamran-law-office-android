"""Original production APK -> same-key 10.2; synthetic fixtures, never uninstall or clear."""
import base64
import hashlib
import json
import os
import pathlib
import subprocess
import urllib.request

REPO = 'kamranvahdati-arch/Kamran-law-office-android'
CERT = '26055f09370416e9cd61c08246b80c57367470cdb6873e282b5b6521cf097202'
PACKAGE = 'ir.kamranvahdati.lawoffice'
out = pathlib.Path('app/build/v102-production-proof')
out.mkdir(parents=True, exist_ok=True)

def materialize(manifest_path, directory):
    manifest = json.loads(pathlib.Path(manifest_path).read_text())
    directory = pathlib.Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    for item in manifest['files']:
        path = directory / item['name']
        if path.parent != directory or path.suffix != '.apk':
            raise ValueError('Unexpected artifact path')
        with path.open('wb') as stream:
            for sha in item['blobs']:
                request = urllib.request.Request(f'https://api.github.com/repos/{REPO}/git/blobs/{sha}', headers={'Authorization': 'Bearer ' + os.environ['GITHUB_TOKEN'], 'Accept': 'application/vnd.github+json'})
                with urllib.request.urlopen(request, timeout=60) as response:
                    stream.write(base64.b64decode(json.load(response)['content']))
        assert path.stat().st_size == item['bytes']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == item['sha256']
    return manifest

def run(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.STDOUT, timeout=300)

def adb(*args):
    return run('adb', *args)

def test(phase):
    result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', PACKAGE + '.V102UpgradeTest', '-e', 'upgrade_phase', phase, PACKAGE + '.test/' + PACKAGE + '.OfficeTestRunner')
    (out / (phase + '.txt')).write_text(result)
    print(result, flush=True)
    if 'OK (1 test)' not in result or 'FAILURES' in result or 'Process crashed' in result:
        (out / 'failure-logcat.txt').write_text(adb('logcat', '-d', '-v', 'threadtime'))
        raise AssertionError('Production upgrade instrumentation failed')

materialize('release-validation/v101.json', 'original101')
manifest = materialize('release-validation/v102.json', 'candidate102')
baseline = 'original101/VOKANO-10.1-release.apk'
candidate = 'candidate102/VOKANO-10.2-release.apk'
instrumentation = 'candidate102/instrumentation.apk'
signer = pathlib.Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0/apksigner'
for index, apk in enumerate([baseline, candidate, instrumentation]):
    verified = run(str(signer), 'verify', '--print-certs', apk)
    assert CERT in verified
    (out / f'certificate-{index}.txt').write_text(verified)
adb('install', baseline)
adb('install', instrumentation)
test('seed')
adb('shell', 'am', 'force-stop', PACKAGE)
adb('install', '-r', candidate)
test('verify')
(out / 'provenance.json').write_text(json.dumps(manifest, indent=2))

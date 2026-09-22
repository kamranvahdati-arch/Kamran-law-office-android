"""Updates fail closed unless the original signing key and expected certificate are supplied."""
import base64, hashlib, os, pathlib, subprocess, tempfile
names=['VOKANO_RELEASE_KEYSTORE_BASE64','VOKANO_RELEASE_KEY_ALIAS','VOKANO_RELEASE_STORE_PASSWORD','VOKANO_RELEASE_KEY_PASSWORD','VOKANO_RELEASE_CERT_SHA256']
if any(not os.environ.get(name) for name in names):
    raise SystemExit('Required permanent signing secrets are missing; no fallback key is allowed')
with tempfile.TemporaryDirectory(prefix='vokano-release-',dir=os.environ.get('RUNNER_TEMP')) as temp:
    os.chmod(temp,0o700);store=pathlib.Path(temp,'release.p12')
    store.write_bytes(base64.b64decode(os.environ['VOKANO_RELEASE_KEYSTORE_BASE64'],validate=True));os.chmod(store,0o600)
    env=dict(os.environ,VOKANO_RELEASE_KEYSTORE=str(store))
    result=subprocess.run(['keytool','-exportcert','-keystore',str(store),'-alias',env['VOKANO_RELEASE_KEY_ALIAS'],
        '-storepass:env','VOKANO_RELEASE_STORE_PASSWORD'],env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    if result.returncode or hashlib.sha256(result.stdout).hexdigest()!=env['VOKANO_RELEASE_CERT_SHA256'].lower().replace(':',''):
        raise SystemExit('Permanent signing certificate mismatch; build refused')
    subprocess.run(['gradle','--no-daemon','clean','assembleRelease','assembleReleaseAndroidTest',
        '-PinstrumentedBuildType=release','-PrequireProductionSigning'],env=env,check=True)

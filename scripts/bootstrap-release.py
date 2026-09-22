"""One-time identity creation, encrypted recovery, and signed build. Never run for updates.
Only the public recovery key is versioned. A unique draft release is the durable creation lock.
"""
import base64, hashlib, io, json, os, pathlib, secrets, shutil, subprocess, tempfile, urllib.request, zipfile
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

repo = os.environ['GITHUB_REPOSITORY']
if repo != 'kamranvahdati-arch/Kamran-law-office-android' or os.environ.get('GITHUB_RUN_ATTEMPT') != '1':
    raise SystemExit('Identity creation is allowed only on the first authorized bootstrap attempt')
event = json.loads(pathlib.Path(os.environ['GITHUB_EVENT_PATH']).read_text())
if event.get('label', {}).get('name') != 'bootstrap-v10-release':
    raise SystemExit('Explicit bootstrap label required')
sha = event['pull_request']['head']['sha']
if subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip() != sha:
    raise SystemExit('Checkout is not the requested source commit')
public = serialization.load_pem_public_key(pathlib.Path('release-bootstrap/recovery-public.pem').read_bytes())
if public.key_size < 4096:
    raise SystemExit('Recovery public key is too short')
out = pathlib.Path(os.environ['RUNNER_TEMP']) / 'vokano-release-output'
out.mkdir(exist_ok=True)

def api(url, data, content_type='application/json'):
    request = urllib.request.Request(url, data=data, method='POST', headers={
        'Authorization': 'Bearer ' + os.environ['GITHUB_TOKEN'],
        'Accept': 'application/vnd.github+json', 'Content-Type': content_type,
        'X-GitHub-Api-Version': '2022-11-28'})
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.load(response)

# Check published and draft locks before creating one. Workflow concurrency serializes creation.
# Do not remove this lock for an update, even if a previous build failed after key creation.
try:
    page=1
    while True:
        request=urllib.request.Request('https://api.github.com/repos/'+repo+'/releases?per_page=100&page='+str(page),headers={
            'Authorization':'Bearer '+os.environ['GITHUB_TOKEN'],'Accept':'application/vnd.github+json'})
        with urllib.request.urlopen(request,timeout=120) as response:
            releases=json.load(response)
        if any(item.get('tag_name')=='vokano-permanent-signing-identity' for item in releases):
            raise RuntimeError('Existing identity lock')
        if len(releases)<100:break
        page+=1
    lock = api('https://api.github.com/repos/' + repo + '/releases', json.dumps({
        'tag_name': 'vokano-permanent-signing-identity', 'target_commitish': sha,
        'name': 'VOKANO permanent signing recovery — encrypted', 'draft': True,
        'body': 'One-time signing identity lock. Encrypted recovery only; no plaintext key or credentials. Preserve this release. Future builds must use the existing key via Secrets.'
    }).encode())
except Exception:
    raise SystemExit('Cannot acquire signing identity lock; no key created. Never delete the lock to retry an update.')

with tempfile.TemporaryDirectory(prefix='vokano-signing-', dir=os.environ['RUNNER_TEMP']) as temp:
    os.chmod(temp, 0o700)
    keystore = pathlib.Path(temp) / 'release.p12'
    password = secrets.token_urlsafe(36)
    alias = 'vokano-' + secrets.token_hex(8)
    # Mask before handing credentials to child processes. These directives are consumed by the runner.
    for value in (password, alias):
        print('::add-mask::' + value, flush=True)
    env = dict(os.environ, VOKANO_RELEASE_KEYSTORE=str(keystore), VOKANO_RELEASE_KEY_ALIAS=alias,
               VOKANO_RELEASE_STORE_PASSWORD=password, VOKANO_RELEASE_KEY_PASSWORD=password)
    result = subprocess.run(['keytool','-genkeypair','-storetype','PKCS12','-keystore',str(keystore),
        '-alias',alias,'-storepass:env','VOKANO_RELEASE_STORE_PASSWORD','-keypass:env','VOKANO_RELEASE_KEY_PASSWORD',
        '-keyalg','RSA','-keysize','4096','-validity','10000','-dname','CN=VOKANO Android Release, O=VOKANO','-noprompt'],
        env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise SystemExit('Key creation failed; signing identity lock retained for investigation')
    os.chmod(keystore, 0o600)
    certificate = subprocess.run(['keytool','-exportcert','-keystore',str(keystore),'-alias',alias,
        '-storepass:env','VOKANO_RELEASE_STORE_PASSWORD'], env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True).stdout
    (out/'release-certificate.der').write_bytes(certificate)
    recovery = io.BytesIO()
    with zipfile.ZipFile(recovery,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('VOKANO-Android-Release.p12',keystore.read_bytes())
        z.writestr('VOKANO-signing-credentials.json',json.dumps({'alias':alias,'store_password':password,
            'key_password':password,'application_id':'ir.kamranvahdati.lawoffice','certificate_sha256':hashlib.sha256(certificate).hexdigest()},indent=2))
    aes = AESGCM.generate_key(bit_length=256);nonce=secrets.token_bytes(12)
    encrypted = AESGCM(aes).encrypt(nonce,recovery.getvalue(),b'VOKANO-KEY-RECOVERY-1')
    wrapped = public.encrypt(aes,padding.OAEP(mgf=padding.MGF1(hashes.SHA256()),algorithm=hashes.SHA256(),label=None))
    envelope = json.dumps({'format':'VOKANO-KEY-RECOVERY-1','wrapped_key':base64.b64encode(wrapped).decode(),
        'nonce':base64.b64encode(nonce).decode(),'ciphertext':base64.b64encode(encrypted).decode()}).encode()
    (out/'signing-recovery.vke').write_bytes(envelope)
    # Preserve recovery BEFORE building. No plaintext signing material is uploaded.
    api(lock['upload_url'].split('{')[0]+'?name=signing-recovery.vke',envelope,'application/octet-stream')
    subprocess.run(['gradle','--no-daemon','clean','assembleRelease','assembleReleaseAndroidTest',
        '-PinstrumentedBuildType=release','-PrequireProductionSigning'],env=env,check=True)
    apksigner = sorted(pathlib.Path(os.environ['ANDROID_HOME'],'build-tools').glob('*/apksigner'),key=lambda x:x.parent.name)[-1]
    subprocess.run([str(apksigner),'verify','--verbose','--print-certs','app/build/outputs/apk/release/app-release.apk'],check=True)
    shutil.copy2('app/build/outputs/apk/release/app-release.apk',out/'VOKANO-10-release.apk')
    shutil.copy2('app/build/outputs/apk/androidTest/release/app-release-androidTest.apk',out/'instrumentation.apk')
subprocess.run(['git','archive','--format=zip','--prefix=VOKANO-10/','-o',str(out/'VOKANO-10-source.zip'),sha],check=True)
(out/'provenance.json').write_text(json.dumps({'source_commit':sha,'source_tree':subprocess.check_output(['git','rev-parse','HEAD^{tree}'],text=True).strip(),
    'version_name':'10.0','version_code':13,'application_id':'ir.kamranvahdati.lawoffice',
    'certificate_sha256':hashlib.sha256(certificate).hexdigest(),'files':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in out.iterdir() if p.is_file()}},indent=2))
print('Permanent signing identity created; encrypted recovery preserved; signed APK built. Device tests still required.')

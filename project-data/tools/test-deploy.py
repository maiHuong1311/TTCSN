"""Exercise deploy.sh with fake Docker/AWS commands; never connect to a server."""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
BASH = os.environ.get('TEST_BASH') or shutil.which('bash')
if os.name == 'nt' and Path('C:/Program Files/Git/bin/bash.exe').exists():
    BASH = 'C:/Program Files/Git/bin/bash.exe'
BASE = ROOT / 'project-data/.local/deploy-script-tests'
BASE.mkdir(parents=True, exist_ok=True)

DOCKER = r'''#!/usr/bin/env bash
set -eu
echo "$*" >> "$TEST_LOG"
if [[ " $* " == *' compose '* ]]; then
  [[ -n "${FORME_IMAGE:-}" ]] || { echo 'FORME_IMAGE lost across sudo' >&2; exit 88; }
  echo "IMAGE=$FORME_IMAGE" >> "$TEST_LOG"
fi
case " $* " in
  *' info '*)
    [[ "${TEST_SUDO:-0}" != 1 || "${MOCK_IN_SUDO:-0}" == 1 ]]; exit ;;
  *' compose version --short '*)
    [[ "$TEST_MODE" != missing-compose ]] || exit 125
    echo '2.40.3'; exit 0 ;;
  *' login '*) cat >/dev/null; exit 0 ;;
  *' config --quiet '*) exit 0 ;;
  *' pull backend '*) [[ "$TEST_MODE" != pull-fails ]]; exit ;;
  *' ps -aq backend '*) [[ "$TEST_OLD" == 0 ]] || echo 'old-container'; exit 0 ;;
  *' inspect '*) echo '331545064056.dkr.ecr.ap-southeast-1.amazonaws.com/forme-backend:previous'; exit 0 ;;
  *' up '*)
    n=0; [[ ! -f "$TEST_COUNTER" ]] || n=$(cat "$TEST_COUNTER")
    n=$((n+1)); echo "$n" > "$TEST_COUNTER"
    if [[ "$TEST_MODE" == success || ( "$TEST_MODE" == recover && "$n" -gt 1 ) ]]; then exit 0; fi
    exit 1 ;;
  *' stop backend '*) exit 0 ;;
esac
echo 'Unexpected mock docker arguments' >&2
exit 99
'''


class DeployTests(unittest.TestCase):
    def run_deploy(self, mode='success', old=True, jwt=None, tag='a' * 40, sudo=False):
        self.temp = tempfile.TemporaryDirectory(dir=BASE)
        self.addCleanup(self.temp.cleanup)
        work = Path(self.temp.name)
        shutil.copy2(ROOT / 'backend/deploy.sh', work / 'deploy.sh')
        shutil.copy2(ROOT / 'backend/docker-compose.yml', work / 'docker-compose.yml')
        secret = jwt if jwt is not None else '0123456789abcdef' * 4
        (work / '.env').write_text(
            'DB_URL=jdbc:mysql://127.0.0.1:3306/forme_store?a=1&b=2\n'
            'DB_USERNAME=forme_app\nDB_PASSWORD=literal$(false)$dollar\n'
            f'JWT_SECRET={secret}\n', encoding='utf-8')
        commands = work / 'commands'
        commands.mkdir()
        mocks = {
            'docker': DOCKER,
            'aws': '#!/usr/bin/env bash\necho dummy-token\n',
            'sudo': '#!/usr/bin/env bash\nset -eu\n[[ "$1" != -n ]] || shift\nunset FORME_IMAGE\nexport MOCK_IN_SUDO=1\nexec "$@"\n',
            'flock': '#!/usr/bin/env bash\nexit 0\n',
            'stat': '#!/usr/bin/env bash\necho 10001:10001\n',
            'python3': '#!/usr/bin/env bash\nexec ' + "'" + sys.executable.replace('\\', '/') + "'" + ' "$@"\n',
        }
        for name, content in mocks.items():
            path = commands / name
            path.write_text(content, encoding='utf-8', newline='\n')
            path.chmod(0o755)
        env = os.environ.copy()
        env.update(MOCK_BIN=commands.as_posix(), TEST_LOG=(work / 'calls').as_posix(),
                   TEST_COUNTER=(work / 'counter').as_posix(), TEST_MODE=mode,
                   TEST_OLD='1' if old else '0', TAG=tag, TEST_SUDO='1' if sudo else '0')
        prefix = '$(cygpath -u "$MOCK_BIN")' if os.name == 'nt' else '$MOCK_BIN'
        command = f'export PATH="{prefix}:$PATH"; exec bash ./deploy.sh "$TAG"'
        result = subprocess.run([BASH, '-c', command], cwd=work, env=env,
                                capture_output=True, text=True, timeout=30)
        self.assertNotIn(secret, result.stdout + result.stderr)
        calls = (work / 'calls').read_text() if (work / 'calls').exists() else ''
        return result, work, calls

    def test_success_records_image_without_down_or_database_commands(self):
        result, work, calls = self.run_deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue((work / '.deploy/current-image').read_text().strip().endswith('a' * 40))
        self.assertNotIn(' down ', calls)
        self.assertNotIn('mysql', calls)

    def test_failed_new_image_restores_previous_and_reports_failure(self):
        result, work, calls = self.run_deploy('recover')
        self.assertEqual(result.returncode, 1)
        self.assertEqual((work / 'counter').read_text().strip(), '2')
        self.assertTrue((work / '.deploy/current-image').read_text().strip().endswith(':previous'))

    def test_first_failure_stops_only_backend_and_keeps_container(self):
        result, work, calls = self.run_deploy('fail', old=False)
        self.assertEqual(result.returncode, 1)
        self.assertIn('stop backend', calls)
        self.assertNotIn(' rm ', calls)

    def test_failed_pull_does_not_replace_existing_container(self):
        result, work, calls = self.run_deploy('pull-fails')
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn(' up ', calls)
        self.assertNotIn('stop backend', calls)

    def test_failed_rollback_still_reports_failure(self):
        result, work, calls = self.run_deploy('fail')
        self.assertEqual(result.returncode, 1)
        self.assertIn('Rollback also failed', result.stderr)

    def test_invalid_env_fails_before_docker_is_touched(self):
        result, work, calls = self.run_deploy(jwt='short')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, '')

    def test_invalid_tag_fails_before_docker_is_touched(self):
        result, work, calls = self.run_deploy(tag='bad;echo-invalid')
        self.assertEqual(result.returncode, 2)
        self.assertEqual(calls, '')

    def test_missing_compose_reports_install_command_without_replacing_container(self):
        result, work, calls = self.run_deploy('missing-compose')
        self.assertEqual(result.returncode, 1)
        self.assertIn('sudo apt-get install -y docker-compose-v2', result.stderr)
        self.assertNotIn(' up ', calls)

    def test_sudo_keeps_image_for_compose_without_preserving_all_env(self):
        result, work, calls = self.run_deploy(sudo=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('IMAGE=331545064056.dkr.ecr.ap-southeast-1.amazonaws.com/forme-backend:' + 'a' * 40, calls)

    def test_sudo_rollback_receives_previous_image(self):
        result, work, calls = self.run_deploy('recover', sudo=True)
        self.assertEqual(result.returncode, 1)
        self.assertIn('IMAGE=331545064056.dkr.ecr.ap-southeast-1.amazonaws.com/forme-backend:previous', calls)
        self.assertTrue((work / '.deploy/current-image').read_text().strip().endswith(':previous'))


if __name__ == '__main__':
    unittest.main()

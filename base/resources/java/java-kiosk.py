#!/usr/bin/env python3
import argparse
import json
import logging
import os
import shutil
import signal
import subprocess
import sys
import time

# Hardcoded configuration values
SYSTEM_INIT_BIN = "/usr/sbin/init"
DETECT_PRIMARY_CARD_BIN = "/usr/local/bin/detect-primary-card"
GLUON_JAVAFX_PATH = "/opt/javafx-sdk"
JAVA_KIOSK_LOG = "/tmp/java-kiosk.log"


# Helper class for running external process with signal handling and runlevel switching
class Runner(object):
    RUNLEVEL_SWITCH_TIMEOUT = 10
    GRACEFUL_STOP_TIMEOUT = 15

    def __init__(self, *args, logger, **kwargs):
        self._args = args
        self._kwargs = kwargs
        self._logger = logger
        self._stopped = False

    def run(self):
        try:
            # Register signal handlers
            self._logger.debug('Registering signal handlers for clean shutdown...')
            self._register_handlers()

            # Switch to runlevel 3 to stop X11
            self._logger.debug('Switching to runlevel 3 to stop X11...')
            self._run_process([SYSTEM_INIT_BIN, '3'], timeout=self.RUNLEVEL_SWITCH_TIMEOUT)

            # Launch JVM with patched options
            self._logger.debug('Launching JVM with previously determined arguments...')
            self._logger.debug(' '.join(*self._args))
            self._run_process(*self._args, **self._kwargs)

            # JVM has exited, proceed with shutdown
            self._logger.debug('JVM process has terminated')
        except KeyboardInterrupt:
            # Silently ignore keyboard interrupts
            # This should already be covered by the signal handler
            self._logger.debug('Swallowed KeyboardInterrupt exception inside run()')
        except subprocess.TimeoutExpired:
            # Silently ignore timeout exceptions
            # This is expected during shutdown procedure
            self._logger.debug('Swallowed TimeoutExpired exception inside run()')
        finally:
            # Switch back to runlevel 5 to start X11
            self._logger.debug('Switching back to runlevel 5 to start X11...')
            self._popen([SYSTEM_INIT_BIN, '5']).wait()

            # All done!
            self._logger.debug('java-kiosk has completed and will now exit')

    def _run_process(self, *args, timeout=None, **kwargs):
        # Abort immediately if we previously stopped already
        if self._stopped:
            return

        # Launch target process
        self._logger.debug('Launching new process with args: %s', args)
        process = self._popen(*args, **kwargs)
        self._logger.debug('Launched new process #%d with args: %s', process.pid, process.args)

        # Busy-wait until process has exited, stopped flag has been set or timeout was hit
        start_time = time.time()
        while not self._stopped and process.poll() is None and (timeout is None or time.time() - start_time < timeout):
            time.sleep(0.1)

        # Return early if process has already stopped
        if process.poll() is not None:
            return

        # Stop the currently running process
        try:
            # Attempt to gracefully terminate application
            self._logger.debug('Attempting graceful shutdown of process #%d...', process.pid)
            process.terminate()
            process.communicate(timeout=self.GRACEFUL_STOP_TIMEOUT)
        except subprocess.TimeoutExpired:
            # Fallback to SIGKILL if application does not respond in time
            self._logger.debug('Forcefully killing process #%d due to timeout...', process.pid)
            process.kill()
            process.communicate()
        finally:
            self._logger.debug('Successfully stopped process')

    def _popen(self, *args, **kwargs):
        kwargs.setdefault('start_new_session', True)
        return subprocess.Popen(*args, **kwargs)

    def _register_handlers(self):
        for sig in (signal.SIGINT, signal.SIGHUP, signal.SIGTERM):
            self._logger.debug('Registering signal handler for %s...', sig)
            signal.signal(sig, self._handle_signal)

    def _handle_signal(self, signum, frame):
        self._stopped = True
        self._logger.debug('Set stopped flag for application runner')


# Helper method to split JVM properties specified as -Dkey=value
def jvm_property(data):
    # Trim -D prefix from property if still present
    if data.startswith('-D'):
        data = data[2:]

    # Split at first equal sign and return as tuple
    parts = tuple(str(data).split('=', 1))
    return parts if len(parts) == 2 else (parts[0], '')


# Parse known arguments and preserve others
parser = argparse.ArgumentParser(description='JavaFX Kiosk Launcher', allow_abbrev=False)
parser.add_argument('--add-modules', default='')
parser.add_argument('-p', '--module-path', default='')
args, unknown_args = parser.parse_known_args()

# Ensure we are running as root
if os.geteuid() != 0:
    parser.error("Unable to execute 'java-kiosk' without running as root")

# Initialize formatter for unified debug logs
formatter = logging.Formatter('%(asctime)s %(levelname)-8s %(message)s')

# Initialize stream handler for logging, only visible when JAVA_KIOSK_DEBUG is set
streamHandler = logging.StreamHandler()
streamHandler.setFormatter(formatter)
streamHandler.setLevel(logging.DEBUG if os.environ.get('JAVA_KIOSK_DEBUG') else logging.INFO)

# Initialize file handler for logging, always active
fileHandler = logging.FileHandler(JAVA_KIOSK_LOG, mode='w')
fileHandler.setFormatter(formatter)
fileHandler.setLevel(logging.DEBUG)

# Initialize logging
logger = logging.getLogger('java-kiosk')
logger.addHandler(streamHandler)
logger.addHandler(fileHandler)
logger.setLevel(logging.DEBUG)

# Search for absolute path of JVM
jvm_path = shutil.which('java')
if jvm_path is None:
    parser.error("Unable to find 'java' binary in current PATH")
logger.debug('Found JVM to launch Java kiosk application: %s', jvm_path)

# Parse Java properties specified as `-D<key>=<value>` or `-D<key>` stored in unknown arguments
# This can not be done with argparse as space or equals sign between argument and value is required
prop_args = list(filter(lambda arg: arg.startswith('-D'), unknown_args))
properties = dict(map(jvm_property, prop_args))
logger.debug('Parsed user-specified JVM properties: %s', prop_args)

# Remove parsed Java properties from unknown arguments
unknown_args = [arg for arg in unknown_args if arg not in prop_args]
logger.debug('Additional JVM arguments: %s', unknown_args)

# Patch '--module-path' option
module_path = list(filter(None, args.module_path.split(':')))
module_path.insert(0, GLUON_JAVAFX_PATH + '/lib')
logger.debug('Patched `--module-path`: %s', module_path)

# Patch '--add-modules' option
add_modules = list(filter(None, args.add_modules.split(',')))
add_modules.insert(0, 'javafx.media')
add_modules.insert(0, 'javafx.controls')
logger.debug('Patched `--add-modules`: %s', add_modules)

# Patch generic properties
properties.setdefault('glass.platform', 'Monocle')
properties.setdefault('embedded', 'monocle')
properties.setdefault('monocle.platform', 'EGL')
properties.setdefault('monocle.platform.traceConfig', 'false')
properties.setdefault('monocle.egl.lib', GLUON_JAVAFX_PATH + '/lib/libgluon_drm.so')
properties.setdefault('javafx.verbose', 'false')
properties.setdefault('prism.verbose', 'false')

# Use auto-detection if egl.displayid was not specified by user
if 'egl.displayid' not in properties:
    # Run helper binary to determine primary video card
    logger.debug('Executing helper for primary video card detection: %s', DETECT_PRIMARY_CARD_BIN)
    result = None
    try:
        result = subprocess.run([DETECT_PRIMARY_CARD_BIN], capture_output=True)
        [logger.debug('> ' + line.decode('utf-8')) for line in result.stderr.splitlines()]
    except Exception as exc:
        logger.debug('Could not run helper executable: %s', exc)

    # Use detect card if successful, otherwise fail
    if result is not None and result.returncode == 0:
        properties['egl.displayid'] = result.stdout.strip().decode('utf-8')
        logger.debug('Auto-detected primary video card: %s', properties['egl.displayid'])
    else:
        parser.error('Could not auto-detect primary video card, please manually specify `-Degl.displayid=/dev/dri/card<X>` property')
else:
    logger.debug('Skipping auto-detection of primary video card due to user-specified value: %s', properties['egl.displayid'])

# Log final set of JVM properties
logger.debug('Patched JVM properties: %s', properties)

# Patch 'java.library.path' property
java_library_path = list(filter(None, properties.get('java.library.path', '').split(':')))
java_library_path.insert(0, GLUON_JAVAFX_PATH + '/lib')
properties['java.library.path'] = ':'.join(java_library_path)
logger.debug('Patched `java.library.path`: %s', java_library_path)

# Patch environment variables
jvm_env = os.environ.copy()
jvm_env['ENABLE_GLUON_COMMERCIAL_EXTENSIONS'] = 'true'
logger.debug('Patched environment: %s', jvm_env)

# Build final list of JVM arguments
jvm_args = [
    '--module-path', ':'.join(module_path),
    '--add-modules', ','.join(add_modules),
]
jvm_args.extend(['-D' + key + '=' + value for key, value in properties.items()])
jvm_args.extend(unknown_args)
logger.debug('Final JVM arguments: %s', jvm_args)

# Attempt to store arguments of current java-kiosk invocation for java-last-kiosk
try:
    if not os.environ.get('JAVA_KIOSK_VOLATILE'):
        persistence_path = os.path.join(os.path.expanduser('~'), '.java-last-kiosk')
        logger.debug('Determined path to last-java-kiosk persistence file: %s', persistence_path)
        with open(persistence_path, 'w') as persistence_file:
            json.dump({
                'cwd': os.getcwd(),
                'args': sys.argv,
            }, persistence_file)
        logger.debug('Successfully persisted current invocation for last-java-kiosk')
    else:
        logger.debug('Skipping update of last-java-kiosk persistence file due to volatile mode')
except Exception as exc:
    logger.warning('Could not store current java-kiosk invocation for java-last-kiosk: %s', exc)

# Run application in kiosk mode
runner = Runner([jvm_path] + jvm_args, env=jvm_env, logger=logger)
runner.run()

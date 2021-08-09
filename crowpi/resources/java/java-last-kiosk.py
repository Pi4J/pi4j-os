#!/usr/bin/env python3
import argparse
import json
import logging
import os
import shlex
import shutil
import sys

# Hardcoded configuration values
JAVA_LAST_KIOSK_LOG = "/tmp/java-last-kiosk.log"

# Parse CLI arguments
parser = argparse.ArgumentParser(description='JavaFX Last Kiosk Launcher', allow_abbrev=False)
parser.add_argument('-d', '--debug', action='store_true', help='Enable debug logging')
parser.add_argument('-n', '--dry-run', action='store_true', help='Only output command, do not execute')
args = parser.parse_args()

# Ensure we are running as root
if os.geteuid() != 0:
    parser.error("Unable to execute 'java-kiosk' without running as root")

# Initialize formatter for unified debug logs
formatter = logging.Formatter('%(asctime)s %(levelname)-8s %(message)s')

# Initialize stream handler for logging, only visible when debug argument was given
streamHandler = logging.StreamHandler()
streamHandler.setFormatter(formatter)
streamHandler.setLevel(logging.DEBUG if args.debug else logging.INFO)

# Initialize file handler for logging, always active
fileHandler = logging.FileHandler(JAVA_LAST_KIOSK_LOG, mode='w')
fileHandler.setFormatter(formatter)
fileHandler.setLevel(logging.DEBUG)

# Initialize logging
logger = logging.getLogger('java-kiosk')
logger.addHandler(streamHandler)
logger.addHandler(fileHandler)
logger.setLevel(logging.DEBUG)

# Search for absolute path of java-kiosk
java_kiosk_path = shutil.which('java-kiosk')
if java_kiosk_path is None:
    parser.error("Unable to find 'java-kiosk' binary in current PATH")
logger.debug("Found path to 'java-kiosk' helper script: %s", java_kiosk_path)

# Determine path to persistence file
persistence_path = os.path.join(os.path.expanduser('~'), '.java-last-kiosk')
logger.debug('Determined path to last-java-kiosk persistence file: %s', persistence_path)

# Parse persistence file as JSON to determine arguments
try:
    with open(persistence_path, 'r') as persistence_file:
        java_kiosk_data = json.load(persistence_file)
except Exception as exc:
    logger.error("Unable to open persistence file: %s", exc)
    logger.error("Please ensure that 'java-kiosk' was executed successfully before")
    parser.error("Unable to continue, arguments for previous 'java-kiosk' invocation are missing")

# Log previously used java-kiosk arguments
logger.debug("Determined previous 'java-kiosk' working directory: %s", java_kiosk_data['cwd'])
logger.debug("Determined previous 'java-kiosk' arguments: %s", java_kiosk_data['args'])

# Either execute java-kiosk or output final command
if not args.dry_run:
    # Adjust working directory
    logger.debug('Switching work directory to previous location: %s', java_kiosk_data['cwd'])
    os.chdir(java_kiosk_data['cwd'])

    # Modify environment for java-kiosk to skip persistence update and optionally enable debug
    java_kiosk_env = os.environ.copy()
    java_kiosk_env['JAVA_KIOSK_VOLATILE'] = 'true'
    java_kiosk_env['JAVA_KIOSK_DEBUG'] = 'true' if args.debug else ''
    logger.debug("Patched environment for 'java-kiosk': %s", java_kiosk_env)

    # Use execve() to replace current process with java-kiosk
    exec_binary, exec_argv = java_kiosk_data['args'][0], java_kiosk_data['args']
    logger.debug("Executing into [%s] using previous argv: %s", exec_binary, exec_argv)
    os.execve(exec_binary, exec_argv, java_kiosk_env)
else:
    print(' '.join(map(lambda arg: shlex.quote(arg), java_kiosk_data['args'])))

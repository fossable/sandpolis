from sys import platform

import os

# Configuration
# PLACEHOLDER

base = config.get('path.' + platform, os.path.expanduser('~') + '/.sandpolis')
lib = base + '/lib'

try:
    os.makedirs(lib)
except FileExistsError:
    pass

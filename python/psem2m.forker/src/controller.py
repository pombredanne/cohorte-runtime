#!/usr/bin/python3
#-- Content-Encoding: utf-8 --
"""
Common classes, methods and constants for the PSEM2M scripts

@author: Thomas Calmant
"""

import psutil

import inspect
import json
import logging
import os
import socket
import subprocess
import sys

if sys.version_info >= (3, 0):
    # Python 3
    import http.client as httplib

else:
    # Python 2
    import httplib

_logger = logging.getLogger("PSEM2M Controller")

# ------------------------------------------------------------------------------

def read_file_line(base, filename):
    """
    Reads the first non-commented line of the file $base/var/$filename, or
    returns None
    
    :param base: The PSEM2M instance base directory
    :param filename: The file to read
    :return: The first line of the file, or None
    """
    if not base:
        print("Invalid base directory")
        return None

    if not filename:
        print("Invalid file name")
        return None

    # Get the file path
    data_file = os.path.join(base, "var", "forker.pid")
    if not os.path.isfile(data_file):
        # File not found
        return None

    # Read it
    with open(data_file) as fp:
        while True:
            # Read the line
            line = fp.readline()
            if not line:
                # Empty line : end of file
                return None

            line = line.strip()
            if not line.startswith("#"):
                # Ignore comments
                return line

    return None


def get_forker_process(base):
    """
    Retrieves the psutil Process object of the forker from the PSEM2M base
    directory information. Returns None if the process is not running.
    
    :param base: The PSEM2M instance base directory
    :return: The forker Process or None
    """
    pid_line = read_file_line(base, "forker.pid")
    if not pid_line:
        # No valid line found
        return None

    try:
        pid = int(pid_line)

    except Exception as ex:
        print("Error reading the forker PID file : %s" % ex)
        return None

    try:
        # Get the process
        return psutil.Process(pid)

    except psutil.NoSuchProcess:
        # Process not found
        return None

# ------------------------------------------------------------------------------

SERVLET_PATH = "/psem2m-signal-receiver"
SIGNAL_NAME = "/psem2m-forker-control-command"

def send_cmd_signal(base, cmd):
    """
    Sends the given signal to the forker
    
    :param base: The PSEM2M instance base directory
    :param cmd: The command to send to the forker
    :return: True if the signal was successfully sent
    """
    # Get the forker access info
    access_info = read_file_line(base, "forker.access")
    if not access_info:
        return False

    # Read the access info line
    access_info = json.loads(access_info)
    host = access_info["host"]
    port = access_info["port"]

    # Set up the signal content
    json_signal = json.dumps(cmd)

    # Prepare request content
    headers = {"Content-Type": "application/json"}
    try:
        # 1 second timeout, to avoid useless waits
        conn = httplib.HTTPConnection(host, port, timeout=1)

        # Setup the signal URI
        signal_url = "%s%s" % (SERVLET_PATH, SIGNAL_NAME)

        conn.request("POST", signal_url, json_signal, headers)
        response = conn.getresponse()
        if response.status != 200:
            _logger.warn("Incorrect response for %s : %s %s",
                         signal_url, response.status,
                         response.reason)

    except socket.error as ex:
        # Socket error
        _logger.error("Error sending command to %s : %s", signal_url, str(ex))

    except:
        # Other error...
        _logger.exception("Error sending command to %s", signal_url)

# ------------------------------------------------------------------------------

ISOLATE_FORKER_ID = "org.psem2m.internals.isolates.forker"

class Main(object):
    """
    Entry point class
    """
    def __init__(self):
        """
        Constructor
        
        :raise ValueError: Invalid environment variables
        """
        self.home = os.getenv("PSEM2M_HOME")
        if not self.home:
            raise ValueError("Invalid PSEM2M_HOME")

        self.base = os.getenv("PSEM2M_BASE", self.home)
        if not self.base:
            raise ValueError("Invalid PSEM2M_BASE")


    def start(self, extra_args=None):
        """
        Starts the platform
        """
        if self._is_running():
            # Name tests have been done, use the process directly
            process = get_forker_process(self.base)
            print("Forker is already running, PID: %d", process.pid)
            return 1

        # Forker needs to be started...
        # FIXME: update the name of the runner...
        args = [sys.executable, "-m", "psem2m.forker.run"]

        # Set up environment
        env = os.environ.copy()
        env["PSEM2M_ISOLATE_ID"] = ISOLATE_FORKER_ID

        # TODO: setup the Python path
        # env["PYTHONPATH"] = os.sep.join(self.home, self.base) ...

        # Run !
        print("Starting forker...")
        try:
            # TODO: change user before Popen
            subprocess.Popen(args, executable=args[0], env=env, close_fds=True)
            return 0
        except:
            _logger.exception("Error starting the forker")
            return 1

        # TODO: wait a little, then send the order to start the monitor ...
        # ... or add a parameter to the forker to detect if it must be started


    def stop(self):
        """
        Stops the platform
        """
        process = get_forker_process(self.base)
        if process is not None:
            # A process with the same PID is running (refreshed PID ?)
            if "python" in process.executable and "forker" in process.cmdline:
                # Forker is running
                if send_cmd_signal(self.base, "stop"):
                    # Signal sent
                    print("Stop command sent")
                    return 0

                else:
                    # Error
                    print("Error sending stop command")
                    return 1

        print("Forker is not running...")
        return 1


    def restart(self):
        """
        Restarts the platform
        """
        self.stop()
        return self.start()


    def _is_running(self):
        """
        Tests if the forker is running
        
        :return: True if the forker is running
        """
        process = get_forker_process(self.base)

        if process is not None:
            # A process with the same PID is running (refreshed PID ?)
            if "python" in process.executable and "forker" in process.cmdline:
                # Forker is running
                return True

        return False


    def try_restart(self):
        """
        The platform must be restarted only if it is actually running
        """
        if self._is_running():
            print("Platform is started...")
            return self.restart()

        else:
            print("Platform is not running")

        return 1


    def force_reload(self):
        """
        Forces the application to reload its configuration or to restart
        """
        return self.restart()


    def reload(self):
        """
        The application must reload its configuration (or restart)
        """
        return self.restart()


    def status(self):
        """
        Prints any relevant status info, and return a status code, an integer:
    
        0          program is running or service is OK
        1          program is dead and /var/run pid file exists
        2          program is dead and /var/lock lock file exists
        3          program is not running
        4          program or service status is unknown
        5-99      reserved for future LSB use
        100-149      reserved for distribution use
        150-199      reserved for application use
        200-254      reserved
    
        @see: http://dev.linux-foundation.org/betaspecs/booksets/LSB-Core-generic/LSB-Core-generic/iniscrptact.html
        """

        if self._is_running():
            print("Platform is running")
            return 0

        else:
            print("Platform seems to be stopped")
            return 3

# ------------------------------------------------------------------------------

def print_usage():
    """
    Prints out the script usage
    """
    print("""%s (start|stop|status|restart|try-restart|reload|force-reload)

    start        : Starts the PSEM2M platform
    stop         : Stops the PSEM2M platform
    status       : Prints the platform execution status
    restart      : (Re)starts the PSEM2M platform
    try-restart  : Restarts the PSEM2M platform if it is already running
    reload       : (Re)starts the PSEM2M platform
    force-reload : (Re)starts the PSEM2M platform
""" % sys.argv[0])


def main(argv):
    """
    Entry point
    """
    if len(argv) < 2:
        print_usage()
        sys.exit(1)

    # Compute action name
    action = argv[1].strip().lower().replace("-", "_")

    try:
        app = Main()

    except ValueError as ex:
        print("Error starting controller : %s" % str(ex))
        sys.exit(1)

    if not hasattr(app, action):
        # Unknown action
        print_usage()
        sys.exit(1)

    # Get the implementation
    action_impl = getattr(app, action)
    nb_action_args = len(inspect.getargspec(action_impl).args)

    # Parse action arguments, if any
    if nb_action_args == 2:
        # Accepts an array of arguments
        # 2 arguments : self + args
        args = argv[2:]
        sys.exit(action_impl(args))

    elif nb_action_args == 1:
        # No extra arguments or expects default values
        # 1 argument : self
        sys.exit(action_impl())


if __name__ == "__main__":
    main(sys.argv)


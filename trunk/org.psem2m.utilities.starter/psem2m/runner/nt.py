#-- Content-Encoding: utf-8 --
"""
Utility methods implementations for Win32

@author: Thomas Calmant
"""

from psem2m.runner.commons import OSSpecificUtils
import os
import psem2m
import psem2m.runner.commons as commons
import pywintypes
import sys
import win32api
import win32process
import winreg

# ------------------------------------------------------------------------------

# From http://msdn.microsoft.com/en-us/library/ms681382%28v=VS.85%29.aspx
ERROR_INVALID_PARAMETER = 0x57

# From http://msdn.microsoft.com/en-us/library/ms684880%28v=VS.85%29.aspx
PROCESS_QUERY_INFORMATION = 0x0400

# From http://msdn.microsoft.com/en-us/library/ms683189%28v=VS.85%29.aspx
STILL_ACTIVE = 259

# ------------------------------------------------------------------------------

def get_registry_java_home():
    """
    Retrieves the value of the JavaHome registry key
    """
    try:
        jre_key_name = r"SOFTWARE\JavaSoft\Java Runtime Environment"
        jre_key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, jre_key_name)

        # Compute current version key name
        value = winreg.QueryValueEx(jre_key, "CurrentVersion")
        if not value:
            print("Warning: No current JVM in registry.")
            return None

        # Close the key
        winreg.CloseKey(jre_key)

        # Get its JavaHome
        current_jre_key_name = jre_key_name + "\\" + value[0]
        jre_key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, \
                                 current_jre_key_name)

        value = winreg.QueryValueEx(jre_key, "JavaHome")
        if not value:
            print("Warning: No current JavaHome in registry.")
            return None

        # Value found
        return commons.remove_quotes(value[0])

    except WindowsError as ex:
        print("Error reading registry :", ex, file=sys.stderr)
        return None


class Utils(OSSpecificUtils):
    """
    Utility class implementation for Win32
    """
    # Java interpreter path under the Java home path
    JAVA_SUBPATH = "bin" + os.sep + "java.exe"

    def __init__(self, psem2m_utils):
        """
        Sets up the utility methods for Win32
        
        @param psem2m_utils: PSEM2M Utilities instance
        """
        OSSpecificUtils.__init__(self, psem2m_utils)


    def find_java_interpreter(self, java_home):
        """
        Finds the Java interpreter, in the given Java Home if possible
        
        @param java_home: The preferred Java home
        """

        # Case 1 : Try "preferred" JVM (embedded one)
        java_home = commons.remove_quotes(java_home)
        java = self._test_java_path(java_home)
        if java != None:
            return java

        # Case 2 : Use registry
        java_home = get_registry_java_home()
        java = self._test_java_path(java_home)
        if java != None:
            return java

        # Case 3 : Try with JAVA_HOME environment variable
        java_home = commons.remove_quotes(os.getenv(psem2m.JAVA_HOME))
        java = self._test_java_path(java_home)
        if java != None:
            return java

        return None

    def is_process_running(self, pid):
        """
        Tests if the given process is running
        
        @param pid: PID of the process to test
        """
        if pid < 0:
            # Invalid PID
            return False

        try:
            # Windows loves handles
            handle = win32api.OpenProcess(PROCESS_QUERY_INFORMATION, False, pid)

        except pywintypes.error as ex:
            # PID not in the system anymore
            if ex.winerror == ERROR_INVALID_PARAMETER:
                return False

            # Other kind of exception
            raise ex

        if not handle:
            # OpenProcess failed
            return False

        # Look at the process state
        exit_code = win32process.GetExitCodeProcess(handle)

        # Clean the place before leaving
        win32api.CloseHandle(handle)

        # Return real state
        return exit_code == STILL_ACTIVE



    def _test_java_path(self, java_home):
        """
        Tries to return the path to a Java interpreter
        
        @param java_home: The Java home to test
        @return: The Java interpreter path or None
        """
        if not java_home:
            return None

        java = os.path.join(java_home, Utils.JAVA_SUBPATH)
        if commons.is_file(java):
            return java

        return None
#!/usr/bin/env python
# -- Content-Encoding: UTF-8 --
"""
Process watchers package

I/0 threads and process waiter component

:author: Thomas Calmant
:license: GPLv3
"""

# Documentation strings format
__docformat__ = "restructuredtext en"

# Boot module version
__version__ = "1.0.0"

# ------------------------------------------------------------------------------

# Cohorte
import cohorte.forker
import cohorte.utils

# Pelix framework
from pelix.ipopo.decorators import ComponentFactory, Validate, Invalidate, \
    Provides, Requires, Instantiate
import pelix.threadpool

# Python standard library
import logging
import threading

# ------------------------------------------------------------------------------

_logger = logging.getLogger(__name__)

# ------------------------------------------------------------------------------

@ComponentFactory()
@Provides(cohorte.forker.SERVICE_WATCHER)
@Requires('_listeners', cohorte.forker.SERVICE_WATCHER_LISTENER,
          aggregate=True, optional=True)
@Instantiate('cohorte-forker-watcher')
class IsolateWatcher(object):
    """
    Isolate watcher service
    """
    def __init__(self):
        """
        Sets up members
        """
        # Lost isolates listeners
        self._listeners = []

        # Utility methods
        self._utils = None

        # Isolate UID -> Process
        self._isolates = {}

        # Loop control
        self._stop_event = threading.Event()

        # Waiting threads
        self._wait_thread = None

        # UID -> (thread, event)
        self._io_threads = {}

        # Pause between loops (in seconds)
        self._pause = .5

        # Notification thread
        self._pool = pelix.threadpool.ThreadPool(1, logname="IOWatcher-Notify")

        # Some locking
        self.__lock = threading.Lock()


    @Validate
    def _validate(self, context):
        """
        Component validated
        """
        # Clear state
        self._stop_event.clear()

        # Get utility methods
        self._utils = cohorte.utils.get_os_utils()

        # Start the notification thread
        self._pool.start()

        # Start the waiting thread
        self._wait_thread = threading.Thread(target=self.__wait_thread,
                                             name="Isolate-Waiter")
        self._wait_thread.daemon = True
        self._wait_thread.start()


    @Invalidate
    def _invalidate(self, context):
        """
        Component invalidated
        """
        # Set states
        self._stop_event.set()
        for _, event in list(self._io_threads.values()):
            event.set()

        # Wait for threads to stop
        self._wait_thread.join()
        for thread, _ in list(self._io_threads.values()):
            thread.join()

        # Stop the notification thread
        self._pool.stop()

        # Clean up
        self._isolates.clear()
        self._wait_thread = None
        self._io_threads.clear()


    def _notify_listeners(self, listeners, uid):
        """
        Notifies listeners of the loss of an isolate

        :param listeners: Listeners to notify
        :param uid: UID of the lost isolate
        """
        _logger.debug("Notifying listeners...")
        for listener in listeners:
            _logger.debug('... notifying %s', listener)
            listener.handle_lost_isolate(uid)


    def __wait_thread(self):
        """
        Thread that waits for children processes to stop
        """
        while not self._stop_event.wait(self._pause) \
        and not self._stop_event.is_set():

            with self.__lock:
                # Copy the isolates information
                isolates = list(self._isolates.items())

            for uid, process in isolates:
                try:
                    # Check PID presence
                    self._utils.wait_pid(process.pid, .1)

                    # Being here means that the process ended,
                    # ... clean up
                    self.unwatch(uid)

                    # ... notify listeners
                    if self._listeners:
                        listeners = self._listeners[:]
                        self._pool.enqueue(self._notify_listeners,
                                           listeners, uid)

                except cohorte.utils.TimeoutExpired:
                    # Time out expired : process is still there,
                    # continue the loop
                    pass


    def __io_thread(self, uid, process, event):
        """
        Thread that looks for the I/O of the given process

        :param uid: Isolate UID
        :param process: Isolate process
        :param event: Loop control event
        """
        # Setup the logger for this isolate
        logger = logging.getLogger("io_watcher.{0}".format(uid))

        while not event.is_set():
            line = process.stdout.readline()
            if not line:
                break

            try:
                # Try to decode the line
                line = line.decode('UTF-8').rstrip()
            except:
                # Not a string line
                pass

            # In debug mode, print the raw output
            logger.debug(line)


    def watch(self, uid, process, watch_io=True):
        """
        Watch for the given isolate

        :param uid: Isolate UID
        :param process: Isolate process (from subprocess module)
        :param watch_io: If True, start an I/O printer thread
        :raise KeyError: Already known UID
        """
        with self.__lock:
            if uid in self._isolates:
                raise KeyError("Already known UID: {0}".format(uid))

            # Store the process
            self._isolates[uid] = process

            if watch_io:
                # Start the I/O thread
                event = threading.Event()
                thread = threading.Thread(target=self.__io_thread,
                                          args=(uid, process, event),
                                          name="IOWatcher-{0}".format(uid))
                thread.daemon = True
                thread.start()

                # Store it
                self._io_threads[uid] = (thread, event)


    def unwatch(self, uid):
        """
        Stop watching an isolate

        :param uid: An isolate UID
        :raise KeyError: Unknown UID
        """
        with self.__lock:
            # Remove from waited isolates
            self._isolates.pop(uid)

            # Remove from the list of I/O threads
            try:
                thread, event = self._io_threads.pop(uid)

            except KeyError:
                # I/O watcher wasn't requested
                return

        # Stop the I/O thread (out of lock)
        event.set()
        thread.join(.2)

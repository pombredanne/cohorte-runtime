#!/usr/bin/env python
#-- Content-Encoding: UTF-8 --
"""
Created on 9 mars 2012

:author: Thomas Calmant
"""
from pelix.ipopo.decorators import ComponentFactory, Provides, Validate, \
    Invalidate, Instantiate, Requires

import json
import os.path
import socket
import threading

# ------------------------------------------------------------------------------

EXTRA_CONF_FOLDERS = ["/etc/default/psem2m", "/etc/psem2m"]
FILE_MAIN_CONF = "psem2m-application.js"
SUBDIR_CONF = "conf"

SYSTEM_PSEM2M_BASE = "PSEM2M_BASE"
SYSTEM_PSEM2M_HOME = "PSEM2M_HOME"

# ------------------------------------------------------------------------------

import logging
_logger = logging.getLogger(__name__)

# ------------------------------------------------------------------------------

@ComponentFactory("FileFinderFactory")
@Instantiate("FileFinder")
@Provides("org.psem2m.isolates.services.dirs.IFileFinderSvc")
class FileFinder(object):
    """
    The PSEM2M file _finder service
    """
    def __init__(self):
        """
        Constructor
        """
        self.roots = None


    def _internal_find(self, file_name):
        """
        Tries to find the given file in the platform directories. Never returns
        None
        
        :param file_name: Name of the file to search for
        :return: The list of the corresponding files (never null, can be empty)
        """
        if file_name[0] == os.path.sep:
            # os.path.join won't work if the name starts with a path separator
            file_name = file_name[1:]

        possible_names = (os.path.join(root, file_name) for root in self.roots)

        found_files = []

        for filename in possible_names:
            if filename not in found_files and os.path.exists(filename):
                found_files.append(filename)

        return found_files


    def extract_platform_path(self, path):
        """
        Tries to extract a platform root path from the given. Non-None result
        indicates that the given path is a root sub-path.
        
        :param path: Path to be transformed
        :retun: The root-path if any, else None
        """
        if not path:
            return None

        for root in self.roots:
            if path.startswith(root):
                return path[len(root):]

        else:
            return None


    def find(self, file_name, base_file=None):
        """
        Tries to find the given file in the platform folders
        
        Tries in the home, then in the base and finally without prefix (for
        complete paths). The file name must be a path from the root of a PSEM2M
        base folder (home or base), a complete path or a path relative to the
        working directory.
        
        :param file_name: The file to look for (tries its absolute path then its
                          name)
        :param base_file: Base file reference (file_name can be relative to it)
        :return: All found files with the given information, None if none found
        """
        result = []

        if not file_name:
            # Empty file name, avoid useless work
            return result

        if base_file:
            if os.path.isfile(base_file):
                # Base file is a file path, get its directory
                base_file = os.path.dirname(base_file)

            platform_dir = self.extract_platform_path(base_file)
            if platform_dir:
                result.extend(self._internal_find(os.path.join(platform_dir,
                                                               file_name)))

            if base_file:
                result.extend(self._internal_find(os.path.join(base_file,
                                                               file_name)))

        result.extend(self._internal_find(file_name))
        return result


    @Validate
    def validate(self, context):
        """
        Component validated
        
        :param context: The bundle context
        """
        self.roots = [os.path.abspath(directory)
                      for directory in (os.getenv(SYSTEM_PSEM2M_BASE),
                                        os.getenv(SYSTEM_PSEM2M_HOME),
                                        os.getcwd())
                      if directory]


    @Invalidate
    def invalidate(self, context):
        """
        Component invalidated
        
        :param context: The bundle context
        """
        self.roots = None

# ------------------------------------------------------------------------------

class _ApplicationDescription(object):
    """
    Application configuration description
    """
    def __init__(self, app_id, multicast_group):
        """
        Constructor
        
        :param app_id: The application ID (non empty string)
        :param multicast_group: Multicast (address, port) tuple for UDP messages
        :raise ValueError: Invalid application ID
        """
        if not app_id:
            raise ValueError("The application ID must be a non-empty string")

        # Application ID
        self.id = app_id

        # Multicast address
        self.multicast = multicast_group[0]
        self.multicast_port = multicast_group[1]

        # Isolates : ID -> Description
        self.isolates = {}


    def get_application_id(self):
        """
        Retrieves the application ID
        
        :return: the application ID
        """
        return self.id


    def get_isolate(self, isolate_id):
        """
        Retrieves the description of the given isolate, None if not found
        
        :param isolate_id: An isolate ID
        ;return: The isolate description, or None
        """
        return self.isolates.get(isolate_id, None)


    def get_isolate_ids(self):
        """
        Retrieves the list of the found isolate IDs
        
        :return: The list of isolate IDs
        """
        return self.isolates.keys()


    def get_multicast_group(self):
        """
        Retrieves the multicast address to use for UDP messages
        
        :return: the multicast address to use for UDP messages
        """
        return self.multicast


    def get_multicast_port(self):
        """
        Retrieves the multicast port to use for UDP messages
        
        :return: the multicast port to use for UDP messages
        """
        return self.multicast_port

# ------------------------------------------------------------------------------

class _BundleDescription(object):
    """
    Description of a bundle configuration
    """
    def __init__(self, raw_dictionary):
        """
        Constructor
        
        :param raw_dictionary: The raw configuration file dictionary
        """
        self.name = None
        self.optional = False
        self.properties = None

        if isinstance(raw_dictionary, dict):
            # Raw dictionary given by the reader
            self.__raw_dictionary = raw_dictionary

        else:
            # Invalid dictionary
            self.__raw_dictionary = {}


    def get_properties(self):
        """
        Retrieves the bundle properties, or None
        
        :return: The bundle properties
        """
        return self.properties


    def get_raw(self):
        """
        Returns the raw bundle description dictionary
        
        :return: the raw bundle description dictionary
        """
        return self.__raw_dictionary


    def get_symbolic_name(self):
        """
        Returns the bundle symbolic name
        
        :return: the bundle symbolic name
        """
        return self.name


    def is_optional(self):
        """
        Tests if the bundle is optional
        
        :return: True if the bundle is optional
        """
        return self.optional


# ------------------------------------------------------------------------------

class _IsolateDescription(object):
    """
    Description of an isolate configuration
    """
    def __init__(self, isolate_id, raw_dictionary):
        """
        Constructor
        
        :param isolate_id: The isolate ID (non empty string)
        :param raw_dictionary: The raw configuration file dictionary
        :raise ValueError: Invalid isolate ID
        """
        if not isolate_id:
            raise ValueError("Empty isolate ID")

        self.bundles = []
        self.node = None
        self.id = isolate_id
        self.kind = ""
        self.port = 8080

        if isinstance(raw_dictionary, dict):
            # Raw dictionary given by the reader
            self.__raw_dictionary = raw_dictionary

        else:
            # Invalid dictionary
            self.__raw_dictionary = {}


    def add_bundle(self, bundle):
        """
        Adds the bundle in the bundles list if it is not yet there
        
        :param bundle: The bundle to be added
        :return: True on success, False if the bundle was already there
        """
        if bundle is None or bundle in self.bundles:
            return False

        self.bundles.append(bundle)
        return True


    def get_bundles(self):
        """
        Retrieves the list of bundles to be installed in the isolate. Can't be
        null and should'nt be empty.
        
        :return: The list of bundles of the isolate
        """
        return self.bundles


    def get_environment(self):
        """
        Retrieves the configured environment dictionary, or an empty one
        
        :return: The environment dictionary
        """
        return self.__raw_dictionary.get("environment", {})


    def get_id(self):
        """
        Retrieves the isolate ID. Can't be null nor empty.
        
        :return: The isolate ID
        """
        return self.id


    def get_kind(self):
        """
        Retrieves the kind of isolate. Must be a kind handled by the bootstrap,
        namely "equinox", "felix", "python" or "python3". Never returns null,
        can return an empty string.
        
        :return: The kind of isolate
        """
        return self.kind


    def get_node(self):
        """
        Retrieves the name of the node hosting the isolate
        
        :return: The node name
        """
        return self.node


    def get_port(self):
        """
        Retrieves the access port
        
        :return: The port to access the isolate
        """
        return self.port


    def get_raw(self):
        """
        Retrieves the raw isolate description found in the configuration file.
        
        :return: The dictionary read from the configuration 
        """
        return self.__raw_dictionary

# ------------------------------------------------------------------------------

@ComponentFactory("JsonConfigFactory")
@Instantiate("JsonConfig")
@Provides("org.psem2m.isolates.services.conf.ISvcConfig")
@Requires("_finder", "org.psem2m.isolates.services.dirs.IFileFinderSvc")
class JsonConfig(object):
    """
    JSON configuration reader
    """
    def __init__(self):
        """
        Constructor
        """
        self._finder = None
        self._context = None

        # Current description
        self.application = None
        self._current_descr_lock = threading.Lock()
        self._current_descr = None

        # File inclusion stack
        self._include_stack = None


    def _compute_overridden_props(self, json_object, overriding_props):
        """
        Parses the given properties object and overrides it with the given
        properties
        
        :param json_object: A properties JSON object (can't be None)
        :param overriding_props: Overriding properties (can be None)
        :return: The overridden properties
        """
        overridden_props = json_object.get("overriddenProperties", None)
        if not overridden_props:
            return overriding_props

        if overriding_props is not None:
            overridden_props.update(overriding_props)

        return overridden_props


    def _parse_file(self, file_name):
        """
        Parses the given JSON file content
        
        :param file_name: Name of the file to look for
        :return: The JSON content of the file
        :raise IOError: File not found
        """
        if self._include_stack:
            base_file = self._include_stack[-1]

        else:
            base_file = "conf"

        files = self._finder.find(file_name, base_file)
        if not files:
            raise IOError("File not found: %s" % file_name)

        conf_file = os.path.abspath(files[0])
        self._include_stack.append(conf_file)

        with open(conf_file) as fp:
            return json.load(fp)


    def _parse_bundle(self, bundle_object):
        """
        Parses a bundle entry
        
        :param bundle_object: A JSON object describing a bundle
        :return: The description of the bundle
        """
        bundle = _BundleDescription(bundle_object)

        bundle.name = bundle_object.get("symbolicName")
        bundle.optional = bundle_object.get("optional", False)
        bundle.properties = bundle_object.get("properties", None)

        return bundle


    def _parse_bundles(self, bundles_array):
        """
        Parses an array of bundles
        
        :param bundles_array: The array of bundles entries
        :return: The resolved bundles array (without imports)
        """
        new_array = []

        for bundle in bundles_array:
            if 'from' in bundle:
                # Case 1 : Import a list of bundles from another file
                new_array.extend(self._parse_bundles(
                                            self._parse_file(bundle['from'])))

                # Remove the included file from the stack
                if self._include_stack:
                    self._include_stack.pop()

            else:
                # Case 2 : everything is described here
                new_array.append(self._parse_bundle(bundle))

        return new_array


    def _parse_isolate(self, isolate_object, overriding_props):
        """
        Parses an isolate entry
        
        :param isolate_object: A JSON object describing an isolate
        :param overriding_props: Overriding properties (overrides isolate's
                                 ones), can be None
        
        :return: The description of the isolate
        """
        isolate = _IsolateDescription(isolate_object["id"], isolate_object)
        isolate.node = isolate_object.get('node', None)

        if self._include_stack is not None:
            _logger.debug("Reading '%s' from '%s'", isolate.id,
                          self._include_stack[-1])

        else:
            _logger.debug("Reading '%s' from nowhere", isolate.id)

        # The isolate kind can be empty, not None
        isolate.kind = isolate_object.get("kind", "")

        # Get the isolate port
        port_str = isolate_object.get("httpPort", 8080)
        if isinstance(port_str, int):
            # Integer read directly from the JSON file
            port = port_str

        else:
            # The port is not an integer, try to get the port from a service
            # name
            try:
                port = socket.getservbyname(port_str)
            except socket.error:
                # Unreadable port
                raise ValueError("Invalid port '%s' for isolate '%s'" \
                                 % (port_str, isolate.get_id()))

        # Store data
        isolate.port = port

        # Get the isolate bundles
        bundles = self._parse_bundles(isolate_object['bundles'])
        for bundle in bundles:
            # Update the bean representation
            isolate.add_bundle(bundle)

        # Update the raw dictionary
        isolate_object['bundles'] = [bundle.get_raw() for bundle in bundles]

        return isolate


    def _parse_isolates(self, isolates_array):
        """
        Parses an array of isolates
        
        :param isolates_array: The array of isolates entries
        """
        index = 0
        for isolate in isolates_array:
            # Compute overriding properties
            overriding_props = self._compute_overridden_props(isolate, None)

            if "from" in isolate:
                # Case 1 : the isolate is described in another file
                isolate_descr = self._parse_isolate(
                                            self._parse_file(isolate["from"]),
                                            overriding_props)

                # Modify the array (replace the 'from' by its content)
                isolates_array[index] = isolate_descr

                # Remove the included file from the stack
                if self._include_stack:
                    self._include_stack.pop()

            else:
                # Case 2 : everything is described here
                isolate_descr = self._parse_isolate(isolate, overriding_props)

            self.application.isolates[isolate_descr.id] = isolate_descr
            index += 1


    def get_application(self):
        """
        Retrieves the current application description
        
        :return: the current application description
        """
        return self.application


    def get_current_isolate(self):
        """
        Retrieves the currently used isolate configuration
        
        :return: the currently used isolate configuration
        """
        with self._current_descr_lock:
            if self._current_descr is not None:
                # We have a specific configuration
                return self._current_descr

            else:
                return self.application.get_isolate(
                                        self._context.get_property(
                                                        'psem2m.isolate.id'))


    def parse_isolate(self, isolate_config):
        """
        Tries to parse the given isolate configuration. Returns None if the
        configuration format is unknown.
        
        :param isolate_config: An isolate configuration
        :return The isolate description object, or None.
        """
        if not isolate_config:
            return None

        try:
            return self._parse_isolate(isolate_config, None)

        except ValueError:
            return None


    def refresh(self):
        """
        Reloads the configuration
        
        :return: True on success
        """
        self._include_stack = []

        try:
            # Parse the configuration file
            config_root = self._parse_file(FILE_MAIN_CONF)

            multicast_group = (config_root["multicast"],
                               config_root.get("multicastPort", 42000))

            self.application = _ApplicationDescription(config_root["appId"],
                                                       multicast_group)
            self._parse_isolates(config_root["isolates"])
            return True

        except ValueError as ex:
            _logger.error("Error parsing the configuration file: %s", str(ex))
            return False

        except:
            _logger.exception("Error loading the configuration file.")
            return False

        finally:
            del self._include_stack[:]
            self._include_stack = None


    def set_current_isolate(self, isolate_descr):
        """
        Sets the currently used isolate configuration
        
        :param isolate_descr: The current isolate description
        """
        with self._current_descr_lock:
            self._current_descr = isolate_descr


    @Validate
    def validate(self, context):
        """
        Component validation
        
        :param context: The bundle context
        """
        self._context = context
        self.refresh()


    @Invalidate
    def invalidate(self, context):
        """
        Component invalidation
        
        :param context: The bundle context
        """
        self.application = None
        self._current_descr = None
        self._include_stack = None

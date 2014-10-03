#!/usr/bin/env python
# -- Content-Encoding: UTF-8 --
"""
Shell commands to control the top composer

:author: Thomas Calmant
:license: Apache Software License 2.0
:version: 3.0.0

..

    Copyright 2014 isandlaTech

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
"""

# Module version
__version_info__ = (3, 0, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

# Composer
import cohorte.composer

# Shell constants
from pelix.shell import SHELL_COMMAND_SPEC, SHELL_UTILS_SERVICE_SPEC

# iPOPO Decorators
from pelix.ipopo.decorators import ComponentFactory, Requires, Provides, \
    Instantiate

# Standard library
from pprint import pformat
import logging
import json

# ------------------------------------------------------------------------------


@ComponentFactory()
@Requires('_reader', cohorte.SERVICE_FILE_READER)
@Requires('_parser', cohorte.composer.SERVICE_PARSER,
          optional=True)
@Requires('_distributor', cohorte.composer.SERVICE_DISTRIBUTOR_NODE,
          optional=True)
@Requires('_composer', cohorte.composer.SERVICE_COMPOSER_TOP,
          optional=True)
@Requires('_status', cohorte.composer.SERVICE_STATUS_TOP)
@Requires('_utils', SHELL_UTILS_SERVICE_SPEC)
@Provides(SHELL_COMMAND_SPEC)
@Instantiate('cohorte-composer-shell-debug')
class TopComposerCommands(object):
    """
    Shell commands to control the top composer
    """
    def __init__(self):
        """
        Sets up members
        """
        # Injected services
        self._reader = None
        self._utils = None
        self._parser = None
        self._composer = None
        self._distributor = None
        self._status = None
        self.logger = logging.getLogger('shell-composer-top')

    def get_namespace(self):
        """
        Retrieves the name space of this command handler
        """
        return "top"

    def get_methods(self):
        """
        Retrieves the list of tuples (command, method) for this command handler
        """
        return [('read', self.read_file),
                ('dist', self.distribute_composition),
                ('load', self.instantiate_composition),
                ('dump', self.dump_status),
                ('stop', self.stop_composition), ]

    def read_file(self, io_handler, filename="autorun_conf.js", base="conf"):
        """
        Reads a file
        """
        # Read the file content (dictionary)
        data = self._reader.load_file(filename, base)

        # Pretty print
        io_handler.write_line("{0}", json.dumps(data, sort_keys=True,
                                                indent='  ',
                                                separators=(',', ': ')))

    def distribute_composition(self, io_handler,
                               filename="autorun_conf.js", base="conf"):
        """
        Parses a composition and computes its node distribution
        """
        if self._parser is None:
            io_handler.write_line("No parser found.")
            return

        # Load the composition
        composition = self._parser.load(filename, base)

        if self._distributor is None:
            io_handler.write_line("No top distributor found.")
            io_handler.write_line("Composition: {0}", str(composition))
            return

        # Distribute it
        distribution = self._distributor.distribute(composition)

        # Pretty print
        io_handler.write_line('{0}', pformat(distribution, indent=2))

    def instantiate_composition(self, io_handler,
                                filename="autorun_conf.js", base="conf"):
        """
        Instantiates the given composition
        """
        if self._parser is None:
            io_handler.write_line("No parser found.")
            return

        # Load the composition
        composition = self._parser.load(filename, base)

        # Tell the top composer to work
        if self._composer is None:
            io_handler.write_line("No top composer service available.")
            return

        uid = self._composer.start(composition)
        io_handler.write_line("Started composition: {0} -> {1}",
                              composition.name, uid)

    def stop_composition(self, io_handler, uid):
        """
        Kills the distribution with the given UID
        """
        if self._composer is None:
            io_handler.write_line("No top composer service available.")
            return

        try:
            self._composer.stop(uid)
            io_handler.write_line("Composition {0} should be stopped", uid)
        except KeyError:
            io_handler.write_line("Unknown composition: {0}", uid)

    def dump_status(self, io_handler, node=None):
        """
        Dumps the content of status
        """
        if self._status is None:
            io_handler.write_line("No top status service available.")
            return

        if not node:
            io_handler.write_line("{0}", self._status.dump())
        else:
            node_components = self._status.get_components_for_node(node)
            io_handler.write_line("{0}", pformat(node_components))
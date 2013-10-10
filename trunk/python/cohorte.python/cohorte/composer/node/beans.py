#!/usr/bin/env python
# -- Content-Encoding: UTF-8 --
"""
Beans to store the description of an isolate in the Node composer

:author: Thomas Calmant
:copyright: Copyright 2013, isandlaTech
:license: GPLv3
:version: 3.0.0

..

    This file is part of Cohorte.

    Cohorte is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cohorte is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Cohorte. If not, see <http://www.gnu.org/licenses/>.
"""

# Module version
__version_info__ = (3, 0, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

import itertools

# ------------------------------------------------------------------------------

class Isolate(object):
    """
    Represents an isolate to be instantiated
    """
    # The isolate counter
    __counter = itertools.count(1)

    def __init__(self, name=None):
        """
        Sets up members

        :param name: The name of the isolate
        """
        # The configured isolate name
        self.__name = name

        # Proposed name, if the current vote passes
        self.__proposed_name = None

        # Language of components hosted by this isolate
        self.language = None

        # Components hosted by this isolate
        self.__components = set()


    def __str__(self):
        """
        String representation
        """
        if not self.language:
            return "NeutralIsolate"

        return "Isolate({0}, {1})".format(self.__name, self.language)


    def accepted_rename(self):
        """
        Possible name accepted

        :raise ValueError: A name was already given
        """
        if self.__name:
            raise ValueError("Isolate already have a name: {0}" \
                             .format(self.__name))

        self.__name = self.__proposed_name
        self.__proposed_name = None


    def propose_rename(self, new_name):
        """
        Proposes the renaming of the isolate

        :raise ValueError: A name was already given to this isolate
        :return: True if the proposal is acceptable
        """
        if self.__name:
            raise ValueError("Isolate already have a name: {0}" \
                             .format(self.__name))

        if self.__proposed_name:
            return False

        self.__proposed_name = new_name
        return True


    def rejected_rename(self):
        """
        Possible name rejected
        """
        self.__proposed_name = None


    def generate_name(self, node):
        """
        Generates a name for this isolate (to be called) after votes.
        Does nothing if a name was already assigned to the isolate

        :param node: The node name
        :return: The (generated) isolate name
        """
        if not self.__name:
            # Need to generate a name
            self.__name = '{node}-{language}-auto{count:02d}' \
                          .format(node=node, language=self.language,
                                  count=next(Isolate.__counter))

        return self.__name


    @property
    def name(self):
        """
        Returns the name of the isolate
        """
        return self.__name


    @property
    def proposed_name(self):
        """
        Returns the currently proposed name, or None
        """
        return self.__proposed_name


    @property
    def components(self):
        """
        Returns the (frozen) set of components associated to this isolate
        """
        return frozenset(self.__components)


    @property
    def factories(self):
        """
        Returns the (frozen) set of the factories required to instantiate
        the components associated to this isolate
        """
        return frozenset(component.factory for component in self.__components)


    def add_component(self, component):
        """
        Adds a component to the isolate
        """
        if self.language is None:
            # First component tells which language this isolate hosts
            self.language = component.language

        self.__components.add(component)
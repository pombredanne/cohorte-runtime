#!/usr/bin/env python
# -- Content-Encoding: UTF-8 --
"""
Common code for instruments

:author: Thomas Calmant
:copyright: Copyright 2014, isandlaTech
:license: GPLv3
:version: 1.0.0

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
__version_info__ = (1, 0, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

# iPOPO Decorators
from pelix.ipopo.decorators import Property

# Standard library
import os

# ------------------------------------------------------------------------------


@Property('_statics', 'html.statics.virtual', '_static')
@Property('_real_statics', 'html.statics.physical', './_html_statics')
class CommonHttp(object):
    """
    Aggregates instruments and forwards requests
    """
    def __init__(self):
        """
        Sets up members
        """
        # Servlet path
        self._path = None

        # Static files
        self._statics = None
        self._real_statics = None

    def set_servlet_path(self, path):
        """
        Sets the path to the instruments servlet
        """
        self._path = '/' + self.normalize_path(path)

    def normalize_path(self, path):
        """
        Normalizes the given path, i.e. removes its double-slashes

        :param path: Path to normalize
        :return: The path, without double-slashes
        """
        return '/'.join(part for part in path.split('/') if part)

    def get_statics_path(self):
        """
        Returns the path to the static files virtual folder
        """
        return '/'.join((self._path, self._statics))

    def page_not_found(self, response, message, redirect_path=None):
        """
        Sends a 404 error

        :param response: A HTTP response bean
        :param message: Message to print
        :param redirect_path: Path where to redirect the browser
        """
        content = "<p>{0}</p>".format(message)

        if redirect_path:
            content = "{0}\n<p>{1}</p>"\
                .format(content, self.make_link("Back to the index",
                                                redirect_path))

        response.send_content(404, self.make_page("Page not found", content))

    def redirect(self, response, sub_path="/", code=404):
        """
        Redirects the browser to the given sub-path of the servlet

        :param response: A HTTP response bean
        :param sub_path: Servlet sub-path where to redirect the client
        :param code: Error code to use (404 by default)
        """
        redirection = '/' + self.normalize_path("{0}/{1}".join(self._path,
                                                               sub_path))
        response.set_response(404)
        response.set_header("Location", redirection)
        response.end_headers()
        response.write("")

    def make_link(self, text, *parts):
        """
        Prepares a link to a servlet sub path

        :param text: Link text
        :param parts: Parts of the link to generate
        :return: A HTML link string
        """
        target = self.normalize_path('/'.join(str(part) for part in parts))
        return '<a href="{0}/{1}">{2}</a>'.format(self._path, target, text)

    def make_list(self, items, tag="ul"):
        """
        Prepare a list in HTML

        :param items: List of items to show
        :param tag: One of ul or ol
        :return: The list in HTML
        """
        return "<{0}>\n{1}</{0}>" \
            .format(tag, "".join("\t<li>{0}</li>\n".format(str(item))
                                 for item in items))

    def make_page(self, title, body, head=None):
        """
        Makes an HTML page

        :param title: Page title
        :param body: Page body content
        :param head: Page head content (optional)
        """
        return """<!DOCTYPE html>
<html lang="en">
<head>
{head}
<title>{title}</title>
</head>
<body>
<h1>{title}</h1>
{body}
</body>
</html>
""".format(title=title or "", body=body or "", head=head or "")

    def send_static(self, response, filename):
        """
        Sends the given static file
        """
        # Ensure it is a relative path
        if filename[0] == '/':
            filename = filename[1:]

        # Get the filename
        filename = os.path.join(self._real_statics, filename)

        try:
            with open(filename) as filep:
                response.send_content(200, filep.read(), "")
        except:
            self.page_not_found(response,
                                "File not found: {0}".format(filename))

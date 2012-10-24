#!/usr/bin/env python
#-- Content-Encoding: UTF-8 --
"""
PSEM2M Compiler: iPOJO Extension

Adds a Sonar task for the compiled projects.

:author: Thomas Calmant
"""

import compiler.antutils as ant

import os
import subprocess

# ------------------------------------------------------------------------------

EXTENSION_CLASS = 'SonarExt'
""" PSEM2M Compiler extension class """

SONAR_TASK_RESOURCE = 'org/sonar/ant/antlib.xml'
""" XML file in the Ant task Jar to define the task """

SONAR_ANT_TASK = "org.sonar.ant.SonarTask"
""" Sonar Ant task class """

# ------------------------------------------------------------------------------

class SonarExt(object):
    """
    Sonar extension for PSEM2M Compiler
    """
    def __init__(self, parameters):
        """
        Sets up the extension
        
        :param parameters: The PSEM2M Compiler configuration
        """
        self.compilation_name = parameters.get('main', 'name')
        self.ant_task_jar = parameters.get_path('sonar', 'ant.task.jar')
        self.projects_ignored = parameters.get_list('sonar', 'projects.ignored')

        # Custom properties
        self.sonar_props = {}
        for key, value in parameters.items('sonar.properties'):
            if value and key.startswith('sonar.'):
                self.sonar_props[key] = value


    def finalize_bundle(self, document, bundle):
        """
        Called once the whole Ant document has been generated, for late
        operations.
        
        :param document: A Ant DOM Document for the given bundle project
        :param bundle: A Bundle object
        """
        # Add the properties
        ant.add_property(document, 'sonar.sources', '${src}')
        ant.add_property(document, 'sonar.binaries', '${build}')
        ant.add_property(document, 'sonar.projectName', bundle.sym_name)
        ant.add_property(document, 'sonar.sourceEncoding', 'UTF-8')

        for key, value in self.sonar_props.items():
            # Add custom properties
            ant.add_property(document, key, value)

        # Shouldn't be there...
        project_key = ':'.join((self.compilation_name, bundle.sym_name))
        ant.add_property(document, 'sonar.projectKey', project_key)
        ant.add_property(document, 'sonar.version', str(bundle.version))

        # Add the class path
        path = ant.create_element(document, 'path', {'id': 'sonar.libraries'})
        path.appendChild(ant.create_element(document, 'path',
                                            {'refId': 'classpath'}))
        document.documentElement.appendChild(path)

        # Add the target...
        sonar = ant.add_target(document, 'sonar', 'Runs Sonar analysis')

        # Add the task definition
        task_name = 'sonar'
        taskdef = ant.create_element(document, 'taskdef',
                                     {'resource': SONAR_TASK_RESOURCE})

        taskdef.appendChild(ant.create_element(document, 'classpath',
                                               {'path': self.ant_task_jar}))
        sonar.appendChild(taskdef)

        # Add the task
        attrs = {'key': project_key + '-key',
                 'version': str(bundle.version)}
        sonar.appendChild(ant.create_element(document, task_name, attrs))


    def post_build(self, master_file, generated_files):
        """
        Called after a successful compilation
        """
        # Use a copy the generated files list
        scripts = generated_files[:]

        # Remove the master file from the list
        scripts.remove(master_file)

        # Common environment options
        environment = {"JAVA_OPTS": "-Xms128m -Xmx1024m -XX:MaxPermSize=1024m"}

        for script in scripts:
            # Test if the script must be ignored
            project = os.path.basename(os.path.dirname(script))
            if project in self.projects_ignored:
                # Ignored project
                continue

            # Call Ant
            subprocess.call(['ant', '-f', script, 'sonar'], env=environment)
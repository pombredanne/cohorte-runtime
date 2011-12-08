#-- Content-Encoding: utf-8 --
'''
Created on 8 déc. 2011

@author: Thomas Calmant
'''

from auto_builder import BinaryBundleFinder, SourceBundleFinder, Dependencies
from eclipse_reader import EclipseProject
from generator import AntGenerator, FileWriter
import logging
import os.path
import xml.dom.minidom as dom

# TODO: let files paths be configuration
IPOJO_NATURE = "org.ow2.chameleon.eclipse.ipojo.iPojoNature"
IPOJO_ANNOTATIONS_FILE = "/media/Data2/thomas/repos-git/felix-trunk/" \
                    +"ipojo/annotations/target/" \
                    + "org.apache.felix.ipojo.annotations-1.9.0-SNAPSHOT.jar"

IPOJO_ANT_TASK = "org.apache.felix.ipojo.task.IPojoTask"
IPOJO_ANT_TASK_FILE = "/media/Data2/thomas/repos-git/felix-trunk/ipojo/" \
                    + "manipulator/ipojo-ant-task/target/" \
                    + "org.apache.felix.ipojo.ant-1.9.0-SNAPSHOT.jar"


def __load_dependencies(eclipse_projects, libraries_paths):
    """
    Finds source and library bundles with AutoBuilder, and prepares a
    Dependencies object
    
    @param eclipse_projects: An array of EclipseProject objects
    @param libraries_paths: An array of string defining JAR libraries paths
    @return: A dependencies resolver
    """ 
    # Prepare finders
    src_finder = SourceBundleFinder()
    lib_finder = BinaryBundleFinder() 
    
    # Find JAR libraries first
    lib_finder.find(libraries_paths)
    lib_finder.load()
    
    # Find source bundles...
    src_finder.find([project.path for project in eclipse_projects])
    src_finder.load()
    
    # Prepare dependencies resolver
    return Dependencies(lib_finder, src_finder, \
                                lib_finder.target_platform)


def __extend_build_file(eclipse_project):
    """
    Adds missing class path entries and the iPOJO task in Ant build file, if
    needed
    
    @param eclipse_project: The Eclipse project to be extended
    """
    assert isinstance(eclipse_project, EclipseProject)
    
    # If the project has the iPOJO nature, add the annotations JAR file
    needs_ipojo_manipulation = False
    libraries = []
    
    if IPOJO_NATURE in eclipse_project.natures:
        # The project has the iPOJO Nature
        needs_ipojo_manipulation = True
        libraries.append(IPOJO_ANNOTATIONS_FILE)
    
    # TODO: read the .classpath file and add missing libraries.
    
    # Test if a modification is needed...
    if not needs_ipojo_manipulation and not libraries:
        # Nothing to do
        return
    
    # Load the build.xml file
    project_build_xml = eclipse_project.path + os.sep + "build.xml"
    if not os.path.exists(project_build_xml):
        # File not found, do nothing...
        logging.warn("%s: No build.xml file found", eclipse_project.name, \
                     exc_info=True)
        return
    
    # Parse the document
    try:
        ant_doc = dom.parse(project_build_xml)
        
    except:
        logging.warn("%s: Error reading build.xml.", eclipse_project.name, \
                     exc_info=True)
        return
    
    # Append class path entries
    __append_classpath_entries(eclipse_project, ant_doc, libraries)
    
    if needs_ipojo_manipulation:
        # iPOJO manipulation needed : add a target and modify "package"
        __append_ipojo_target(eclipse_project, ant_doc)
    
    # Write down the new file
    with open(project_build_xml, "w") as build_xml:
        build_xml.write(ant_doc.toxml())


def __append_classpath_entries(eclipse_project, ant_document, libraries):
    """
    Appends the given libraries to the class path entries
    
    @param eclipse_project: The Eclipse project
    @param ant_document: Ant document
    @param libraries: Libraries to add in the class path
    """
    if not libraries:
        # Do nothing...
        return

    classpath_node = None
    ant_root = ant_document.documentElement
    
    path_nodes = ant_document.getElementsByTagName("path")
    if path_nodes != None:
        for node in path_nodes:
            if node.getAttribute("id") == "classpath":
                classpath_node = node
    
    if classpath_node == None:
        # Create the node if needed
        classpath_node = ant_document.createElement("path")
        classpath_node.setAttribute("id", "classpath")
        ant_root.appendChild(classpath_node)
    
    # Create path elements
    for library in libraries:
        lib_node = ant_document.createElement("pathElement")
        lib_node.setAttribute("location", library)
        classpath_node.appendChild(lib_node)


def __append_ipojo_target(eclipse_project, ant_document):
    """
    Appends the iPOJO Ant target to the given DOM element
    
    @param eclipse_project: The Eclipse project
    @param ant_document: Ant document
    """
    # Modify the "package" target
    ant_targets = ant_document.getElementsByTagName("target")
    if not ant_targets:
        logging.warn("%s: no Ant target found.", eclipse_project.name)
        return
    
    for target in ant_targets:
        if target.getAttribute("name") == "package":
            package_target = target
            break
    else:
        logging.warn("%s: 'package' target not found.", eclipse_project.name)
        return
    
    # Add the task definition
    task_name = "ipojo"
    task_def = ant_document.createElement("taskdef")
    task_def.setAttribute("name", task_name)
    task_def.setAttribute("classname", IPOJO_ANT_TASK)
    task_def.setAttribute("classpath", IPOJO_ANT_TASK_FILE)
    package_target.appendChild(task_def)
    
    # Find the JAR file
    ant_properties = ant_document.getElementsByTagName("property")
    if not ant_properties:
        logging.warn("%s: No Ant properties in build.xml", eclipse_project.name)
        return
    
    for ant_property in ant_properties:
        if ant_property.getAttribute("name") == "bundle":
            jar_file = ant_property.getAttribute("value")
            break
    else:
        logging.warn("%s: Can't get the project bundle name", \
                     eclipse_project.name)
    
    # Add the task
    ipojo_task = ant_document.createElement(task_name)
    ipojo_task.setAttribute("input", jar_file)
    
    # Metadata file found, use it
    metadata_file = eclipse_project.path + os.sep + "metadata.xml"
    if os.path.exists(metadata_file):
        ipojo_task.setAttribute("metadata", metadata_file)

    package_target.appendChild(ipojo_task)


def prepare_ant_files(compilation_name, root_directory, eclipse_projects, \
                      libraries_paths):
    """
    Generates Ant build.xml files using auto_builder
    
    @param compilation_name: The compilation project name
    @param root_directory: Root compilation directory
    @param eclipse_projects: An array of EclipseProject objects
    @param libraries_paths: An array of string defining JAR libraries paths
    """
    # Resolve dependencies
    dependencies = __load_dependencies(eclipse_projects, libraries_paths)
    dependencies.resolve()
    dependencies.sort()
    
    # Generate Ant files with AutoBuilder
    writer = FileWriter()
    ant_generator = AntGenerator(compilation_name, dependencies.src.bundles, \
                                 dependencies.target_platform, root_directory, \
                                 writer)
    ant_generator.generate_build_files()

    # Complete their class paths
    for project in eclipse_projects:
        __extend_build_file(project)

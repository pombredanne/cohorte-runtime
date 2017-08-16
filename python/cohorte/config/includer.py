#!/usr/bin/env python
# -- Content-Encoding: UTF-8 --
"""
COHORTE file finder

component that allow the resolve a file that contain multiple includes. 
this allow to split a composition file in severals file and get a full resolved file 

:author: Aurélien PISU
:license: Apache Software License 2.0

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

# COHORTE constants
import cohorte
import glob
import itertools
import json
import logging
import os
from pelix.ipopo.decorators import ComponentFactory, Provides, Instantiate, \
    Validate, Invalidate, Requires
import re


try:
    # Python 3
    # pylint: disable=F0401,E0611
    import urllib.parse as urlparse
except ImportError:
    # Python 2
    # pylint: disable=F0401
    import urlparse

# iPOPO Decorators
# ------------------------------------------------------------------------------

# Documentation strings format
__docformat__ = "restructuredtext en"

# Version
__version_info__ = (1, 0, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# ------------------------------------------------------------------------------

_logger = logging.getLogger(__name__)
# ------------------------------------------------------------------------------

regexp_replace_var = re.compile("\\$\\{(.+?)\\}", re.MULTILINE)
def merge_full_dict(dict_1, dict_2):
    dict_res = dict_1.copy()
    _merge_full_dict(dict_res, dict_2)
    return dict_res
    

def _merge_full_dict(dict_1, dict_2):
   
    for k, v2 in dict_2.items():
        v1 = dict_1.get(k)  # returns None if v1 has no value for this key
     
        if (isinstance(v1, dict) and 
             isinstance(v2, dict)):
            merge_full_dict(v1, v2)
        elif isinstance(v1, list) and isinstance(v2, list):
            for value in v2:
                dict_1[k].append(value)
        else:
            dict_1[k] = v2

def peek(iterable):
    try:
      
        first = next(iterable)
    except Exception:
        return None
    return first, itertools.chain([first], iterable)  

def replace_vars(params, contents):
    """
    @param params : a dictionnary 
    @param content : content with variable to be replace 
    @return a string with variable identified by k from the query string by the value 
    """
    if isinstance(contents, str):
        contents = [contents]
    replace_contents = []

    if params != None:
        for content in contents:
            replace_content = content
            for match in regexp_replace_var.findall(content):
                if match in params:
                    replace_content = replace_content.replace("${" + match + "}", params[match][0])
                else:
                    replace_content = replace_content.replace("${" + match + "}", "") 
            replace_contents.append(replace_content)
    return replace_contents



class CBadResourceException(Exception):
    
    def __init__(self, message):
        self._message = message

    def __str__(self):
        return self._message


class CResource(object):
    
    
    
    def get_tag(self):
        return self.tag
    
    def get_params(self):
        return self.parms
    
    def get_path(self):
        return self.path
    
    def getdirpath(self):
        return self.dirpath
    
    def get_contents(self):
        return self.contents
    
    def set_contents(self, contents):
        self.contents = contents
        
  

    def readall(self):
        """
        return the merge content of all files that correspond to the resource
        """
        join_list = []
        if self.contents != None:
            for content in self.contents:
                if self.tag != None:
                    tag_elem = None
                    json_obj = json.loads(content);
                    idx_obraces = self.tag.find("[")
                    idx_cbraces = self.tag.find("]") 
                    tag_key = self.tag
                    if idx_obraces != -1:
                        tag_key = self.tag[:idx_obraces]
                        
                    if not tag_key in json_obj.keys():
                        mess = "include property [{0}]  defined in file [{1}] doesn't exists in file [{2}]".format(tag_key, self.resource_parent.filename, self.filename);
                        raise CBadResourceException(mess)
                    else:
                        arr_elem = json_obj[tag_key]
                    # manage a tag can identified a array or a object and we want a elem of the array or all elemnet or the array
                    if(idx_obraces != -1 and idx_cbraces != -1):
                        if isinstance(arr_elem, list):
                            # want something from the array and not all the array
                            range_idx = self.tag[idx_obraces + 1:idx_cbraces]
                            if range_idx == None:
                                # nothin specify raise an error
                                raise CBadResourceException("include : No index but array ask")

                            elif range_idx.isdigit():
                                # want a specific element
                                idx = int(range_idx)
                                if idx > len(arr_elem):
                                    raise CBadResourceException("include : Bad index expect=[{0}] size=[{1}]".format(idx, len(arr_elem)))
                                else:
                                    tag_elem = json.dumps(arr_elem[idx], indent=2)
                            elif range_idx == "*":
                                tag_elem = ",".join([json.dumps(elem) for elem in arr_elem])
                            else:
                                idxs = range_idx.split(":")
                                first = None
                                last = None
                                if idx[0].isdigit():
                                    first = int(idx[0])
                                if idx[1].isdigit():
                                    last = int(idx[1])
                                if last != None and fist != None:
                                    tag_elem = ",".join([json.dumps(elem) for elem in arr_elem[first:last]])
                                elif last != None:
                                    tag_elem = ",".join([json.dumps(elem) for elem in arr_elem[:last]])
                                else:
                                    tag_elem = ",".join([json.dumps(elem) for elem in arr_elem[first:]])

                                # want a specific range 
                                pass
                        else:
                            raise CBadResourceException("include : expect for json array and get a json object urlinclude=[{0}]".format(self.fullpath))
                    else:
                        tag_elem = json.dumps(json.loads(content)[self.tag])
                        
                    join_list.append(tag_elem)
                else:
                    join_list.append(content)
            
            return ",".join(join_list)
        return None
    
    """
    describe a resource that can be file, a http or memory or any kind
    """
    def __init__(self, filename, finder, parent_resource=None):
        """
        construct a resource from a string e.g file:///mypath/foo.txt?k1=v1&#mytag 
        @param resource : parent resource , use if path is relative 
        @param alternive_dirs : alternative dirs to locate  the file content to include
        @param path : path of the current resource to create 
        """
        self.fullpath = filename
        self.protocol_idx = 0
        # contain the paths possible of location of the file
        self.filename = None
        # list of directory data and base (and home) where the file can be located
        self._finder = finder
        self.params = None
        self.type = "file"
        # contain the subpath of the directory not an absolute path 
        self.dirpath = None
        self.contents = None
        self.tag = None
        self.resource_parent = parent_resource;
        self._set_filename(filename, parent_resource)
    
    
    def _set_filename(self, filename, parent_resource=None):
        """
        @param path : path of the files to load. can identified one file by multiple file via a regexp or wildchar
        """
        if filename != None:
            self.query_idx = filename.find("?")
            self.tag_idx = filename.find("#")    
            filename = self._init_type(filename)
            filename = self._init_filename(filename, parent_resource)
            filename = self._init_query(filename)
            if  self.tag_idx != -1:
                self.tag = filename[self.tag_idx + 1:]
                         
            self.contents = self._init_contents()
        else:
            raise CBadResourceException("path parameter has 'None' value")
    
    def _init_type(self, filename):
        """ 
        return the protocol to use for resolving the url content 
        @param path : a string that start with protocol:// 
        @return : a type that identified which protocol to use : can be file, http or memory
        """
        if filename != None:
            self.type = "file"
             # prefix that identify the protocol e.g file://
            if filename.startswith("memory://"):
                self.type = "memory"
                self.protocol_idx = 9
            elif filename.startswith("http://"):
                self.type = "http"
                self.protocol_idx = 7
            elif filename.startswith("file://"):
                self.protocol_idx = 7

        return filename
    
    
    def _init_query(self, filename):
        """
        return the query string parameter key=value that can be in the url. the substring starts with '#' will be ignored
        @param filename : a string that can contain key value a.g foo?k=v&p=c
        @return : a param dictionnary that contain the key value 
        """
        if self.query_idx != -1:
           
           # set query if exists
            if self.tag_idx != -1:
                query = filename[self.query_idx + 1:self.tag_idx]
            else:
                query = filename[self.query_idx + 1:]
    
            self.params = urlparse.parse_qs(query)
            
            
        return filename
        
    def _init_contents_file(self):
        """
        return the contents of the files identified by the path 
        @return : a list of String 
        """
        contents = []
        # TODO manage c
        
        list_file = self._finder.find_rel(self.filename, self.dirpath)
      
        for files in list_file:
            for file in files:
                with open(file) as obj_file:
                    contents.append("\n".join(obj_file.readlines()))
          
        if len(contents) > 0:              
            return contents
        # no content found in list of directory
        return None
    
    
    def _init_contents(self):
        """
        return the content of the url with the variable replace 
        @return : a  list of String 
        """
        if self.type == "file":
            self.contents = self._init_contents_file()
        else:
            # not manager
            self.content = None
                          
        if self.params != None:
            self.contents = replace_vars(self.params, self.contents)
            
        return self.contents
        
    
    def _init_filename(self, filename, parent_resource=None):
        """
        return the  filename of the resource to read
        @param filename : path of the resource to read 
        @param parent_resource : a parent resource that include the current in order to resolve the relative path if it's necessary
        @return : a String 
        """
        if filename != None:
                 
            res_file_name = None    
            if self.query_idx != -1:
                res_file_name = filename[self.protocol_idx:self.query_idx]
            elif self.tag_idx != -1: 
                res_file_name = filename[self.protocol_idx:self.tag_idx]
            else:
                res_file_name = filename[self.protocol_idx:]
             
            # compute dir path and filename 
            if res_file_name.find(os.sep) != -1:
                split_file = res_file_name.split(os.sep)
                self.dirpath = os.sep.join(split_file[:-1])
                self.filename = split_file[-1]

            else:
                self.dirpath = ""
                self.filename = res_file_name
                
            # check if parent resource has dirpath 
            if  parent_resource != None and parent_resource.dirpath != "":
                self.dirpath = parent_resource.dirpath + os.sep + self.dirpath 
           

    
        return filename
    
   

    
    
@ComponentFactory('cohorte-file-includer-factory')
@Provides(cohorte.SERVICE_FILE_INCLUDER)
@Requires("_finder", cohorte.SERVICE_FILE_FINDER)
@Instantiate('cohorte-file-includer')
class FileIncluder(object):
    """
    Simple component that resolve a comment json that can include using { $include : "path"} a subfile json content
    """
    def __init__(self):
        self._all = re.compile("(/\\*+((\n|/|\\s|\t)*[^\\*][^/](\n|/|\\s|\t)*)*\\*+/)|(.*//.*)", re.MULTILINE)
        self._check = re.compile("(/\\*+((\n|/|\\s|\t)*[^\\*][^/](\n|/|\\s|\t)*)*\\*+/)", re.MULTILINE)
        self._check_slash = re.compile("(\".*//.*\")", re.MULTILINE)
        self._include = re.compile("((\\n)*\\{(\\n)*\\s*\"\\$include\"\\s*:(\\s*\".*\"\\s*)\\})", re.MULTILINE)
        self._finder = None


        
    def _get_content(self, filepath, parent_resource=None):
        """
        return a resolved content json string without commentary
        """
        content = None
        if filepath != None:
            _logger.info("_getContent {0}".format(filepath))
            
            
          
            # return a resolve content json 
            resource = CResource(filepath, self._finder, parent_resource)
            # remove comment and content of the resource
            self._remove_comment(resource)
            
            # resolve content 
            self._resolve_content(resource);
            
                           
            return resource.readall()
          
        return None
    
    def get_content(self, filename, want_json=False):
        """
        @param filename: path of the file or files to read. if it's list of file wanted, we need to hve a regexp or a string with a wildchar or list of file with ";" as separator
        @param wantJson : boolean to defined if we want a json object as a result or a string
        return a resolved content json string without commentary
        """
    
        
        # multi path asked if the filename contains ; separator 
        if filename.find(";") != -1: 
            content = ",".join([self._get_content(name) for name in filename.split(";")])
        else:
            content = self._get_content(filename)
            
        merge_content = None
        if content == None:
            raise IOError("file {0} doesn't exists".format(filename))
        json_contents = json.loads("[" + content + "]")
     
        for json_content in json_contents:
            if isinstance(json_content, dict):
                # must be always a dict to append all json dict
    
                if merge_content == None:
                    merge_content = json.loads("{}")
                merge_content = merge_full_dict(merge_content, json_content)
                
            elif isinstance(json_content, list):
                # must be always a list to append all json arrays
                if merge_content == None:
                    merge_content = json.loads("[]")
                    for arr in json_content:
                        merge_content.append(arr)
                       
                if merge_content == None:
                    raise IOError("{0} doesn't exists ".format(path))
                      
   
        if not want_json:
            return json.dumps(merge_content)
        return merge_content
     
     
      
    
    def _resolve_content(self, resource):
        """
        return a resolve content with all include file content 
        """
        _logger.info("_revolveContent")

        contents = resource.get_contents();
        if contents != None:
            resolved_contents = []
            for content in contents:
                resolved_content = content
                # apply regexp to remove content
                for matches in self._include.findall(resolved_content):
                    _logger.debug("_revolveContent: match found {0}".format(matches))
                    match = matches[0]
                    sub_contents = []
                    # load json to get the path and resolve it
                    match_json = json.loads(match)
                    if match_json != None:
                        if  match_json["$include"] != None:
                            paths = match_json["$include"].split(";")
                            _logger.debug("_revolveContent: paths {0}".format(paths))
    
                            # match is { $include: "file:///path of a file"}
                            for path in paths:
                                _logger.debug("_revolveContent: subContentPath {0}".format(path))
                                sub_content = self._get_content(path, resource)
                                sub_contents.append(sub_content)
                             
                            # replace match by list of subcontent 
                            resolved_content = resolved_content.replace(match, str.join(",", sub_contents))
                resolved_contents.append(resolved_content);
                            
                            
            # check if the json is ok and reformat it for the correct application of the regexp
            resource.set_contents(resolved_contents)         

 
    
    def _remove_comment(self, resource):
        """
        change the content to remove all possible comment // or /* ...*/
        """
        _logger.info("_removeComment")
        contents = resource.get_contents()
        if contents != None:
            # apply regexp to remove content
            contents_no_comment = []
            for content in contents:
                content_no_comment = content
                for matches in self._all.findall(content):
                    for match in matches:
                        if match != None and match.find("/") != -1 and len(match) > 1:
                            _logger.debug("match comment {0}".format(match))
                            if self._check.search(match) != None or  self._check_slash.search(match) == None:  
                                _logger.debug("_removeComment: match found {0}".format(match))
                                idx = match.find("/")
                                content_no_comment = content_no_comment.replace(match[idx:], "")
                    
                content_no_comment = json.dumps(json.loads(content_no_comment), indent=2)
                contents_no_comment.append(content_no_comment)
                       
            resource.set_contents(contents_no_comment)         
       
           

    

    
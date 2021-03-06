#!/usr/bfiles/in/env python
# -- Content-Encoding: UTF-8 --
"""
COHORTE file include unit test 



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

import json
import logging
import os
import unittest

from cohorte.config import finder as finder
from cohorte.config import includer as includer
import  cohorte.config.common as common
from cohorte.config.includer import CBadResourceException

logging.basicConfig(level=logging.INFO)
try:
    # Python 3
    # pylint: disable=F0401,E0611
    import urllib.parse as urlparse
except ImportError:
    # Python 2
    # pylint: disable=F0401
    import urlparse
    
# unit test 
##############@                
_logger = logging.getLogger(__name__)

test_cases_include = [
    ("my_composition.js", "my_composition"),
    ("composition.js", "composition"),
    ("boot-forker.json", "boot-forker"),
    ("merged/boot-forker-merge.js", "boot-forker-merge"),
    ("merged/composer/python-top.js", "python-top") ,
    ("empty.js*", "empty"),
    ("arrayTag.json", "arrayTag"),
    ("arrayTag.js*", "arrayTag"),
    ("module_noComment.json", "noComment"),
    ("module_slashComment.json", "noComment"),
    ("module_slashStarComment.json", "noComment"),
    ("module_allComment.json", "noComment"),
    ("module_testDef.json", "testDef"),
    ("module_allCommentAndFile.json", "noComment2"),
    ("module_allMultiPath.json", "noCommentMutliPath") ,
    ("module_allMultiPathWildChar.json", "noCommentMutliPathWildChar") ,
    ("module_allMultiPathWildCharAndSubProp.json", "noCommentMutliPathWildCharAndProp") ,
    ("module_conditional_include.json", "module_conditional_include") ,

    ("merged/java-*", "merge-java") ,
    ("merged/java-*;merged/composer/*", "merge-java-composer") 

]
test_cases_replace = [
    ("t=2&t2=3", "${t} foo ${t2}", "2 foo 3"),
    ("t=2&", "${t} foo ${t2}", "2 foo "),
    ("t=2", "${t} foo ${t2}", "2 foo "),
    ("t=2&foo=test", "${t} foo ${t2}", "2 foo "),
    ("", "${t} foo ${t2}", " foo ")
]

test_cases_mergedict = [
    (json.loads('{"a":{"b":{"b1":"b2"}},"c":["c1","c2"]}'),
        json.loads('{"a": {"b": {"b3": "b4"}},"d": {"d1": "d2"},"c": ["c3", "c4"]}'),
        json.loads('{"a": {"b": {"b1": "b2", "b3": "b4"}}, "c": ["c3", "c4", "c1", "c2"], "d": {"d1": "d2"}}'))
]


class testIncluder(unittest.TestCase):

    def setUp(self):
        # mock the finder to validate the includer  
        self.finder = finder.FileFinder();
        self.finder._roots = [
            os.path.dirname(__file__) + os.sep + "files" + os.sep + "includer" + os.sep + "in_data",
            os.path.dirname(__file__) + os.sep + "files" + os.sep + "includer" + os.sep + "in_base"
        ]
        self.include = includer.FileIncluder()
        self.include._finder = self.finder

    def test_merge_dict(self):
        _logger.info("test_merge_dict")
        for js1, js2, expect in test_cases_mergedict:
     
            result = common.merge_object(js1, js2)
            print(json.dumps(result))
            self.assertEqual(json.dumps(result), json.dumps(expect))
            _logger.info("====>\t ok : queryString=[{0}]  stringToReplace=[{1}] result=[{2}]".format(js1, js2, result))
        
    def test_replace_var(self):
        _logger.info("test_replace_var")
        for query, to_replace, replaced in test_cases_replace:
            self.assertEqual(common.replace_vars(urlparse.parse_qs(query), [to_replace]), [replaced], "test replace vars")
            _logger.info("====>\t ok : queryString=[{0}]  stringToReplace=[{1}] result=[{2}]".format(query, to_replace, replaced))
        
    def test_include_notexists(self):
        _logger.info("test_include_notexists")
        self.assertRaises(IOError, self.include.get_content, "notexistsfile")
        self.assertRaises(CBadResourceException, self.include.get_content, "badTag.json")

        _logger.info("====>\t ok : Error valid ")

    def test_include(self):
        _logger.info("test_include")
        for file_in, file_out in test_cases_include:
            result = None
            filepath_out = os.path.dirname(__file__) + os.sep + "files" + os.sep + "includer" + os.sep + "out" + os.sep + file_out + ".json"
           
            with open(filepath_out) as file_test:
                expected_result = json.dumps(json.loads("\n".join(file_test.readlines())), indent=4, sort_keys=True)
            caseinfo = "test case {0}".format(file_in)
            try:
                content = self.include.get_content(file_in , True)
        
                result = json.dumps(content, indent=4, sort_keys=True)
                # print(result)
                self.assertEqual(result, expected_result, caseinfo)
                _logger.info("====>\t ok :case " + caseinfo)

            except Exception as e :
                _logger.exception(e)
                _logger.error("====>\t ko :case {} exception={}".format(caseinfo, e))
   

if __name__ == "__main__":  # call all test
   unittest.main()

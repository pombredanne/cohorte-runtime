/*******************************************************************************
 * Copyright (c) 2011 www.isandlatech.com (www.isandlatech.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ogattaz (isandlaTech) - initial API and implementation
 *******************************************************************************/
package org.psem2m.utilities.logging.test;

import org.psem2m.utilities.teststools.CQualityTestBase;

/**
 * @author isandlatech (www.isandlatech.com) - ogattaz
 * 
 */
public class CLogJunitTest extends CQualityTestBase {

	private CLogTester pAppTester;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		pAppTester = new CLogTester(START_ARGS);
		logInfo(this, "startTests", "App=[%s]", pAppTester.getIdentifier());
	}

	/**
	 * @throws Exception
	 */
	public void testA_LogLog() throws Exception {
		logMethodName(this, "testLogLog");

		// kind: log
		// nbLog: 2000
		// filesize: 1 megabytes
		// filecount: 10
		pAppTester.execLine("log log 2000 1m 10");
	}

	/**
	 * @throws Exception
	 */
	public void testB_LogTxt() throws Exception {
		logMethodName(this, "testLogTxt");

		// kind: log
		// nbLog: 5000
		// filesize: 2 megabytes
		// filecount: 10
		pAppTester.execLine("log txt 5000 2m 10");
	}

	/**
	 * @throws Exception
	 */
	public void testC_LogXml() throws Exception {
		logMethodName(this, "testLogXml");

		// kind: xml
		// nbLog: 2000
		// filesize: 1 megabytes
		// filecount: 10
		pAppTester.execLine("log xml 2000 1m 10");
	}
}

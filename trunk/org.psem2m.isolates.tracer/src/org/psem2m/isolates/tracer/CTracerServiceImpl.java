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
package org.psem2m.isolates.tracer;

import java.util.List;

import org.apache.felix.ipojo.Pojo;
import org.psem2m.isolates.base.CPojoBase;
import org.psem2m.isolates.loggers.ILogChannelsService;
import org.psem2m.utilities.CXStringUtils;

/**
 * @author isandlatech (www.isandlatech.com) - ogattaz
 * 
 */
public class CTracerServiceImpl extends CPojoBase implements ITracerService {

	/** Service reference managed by iPojo (see metadata.xml) **/
	private IBundleTracerLoggerService pBundleTracerLoggerService;

	/** Service reference managed by iPojo (see metadata.xml) **/
	private ILogChannelsService pLogChannelsService;

	/**
	 * Explicite default constructor
	 */
	public CTracerServiceImpl() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.utilities.CXObjectBase#destroy()
	 */
	@Override
	public void destroy() {
		// ...
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.isolates.osgi.CPojoBase#getPojoId()
	 */
	@Override
	public String getPojoId() {
		return ((Pojo) this).getComponentInstance().getInstanceName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.isolates.osgi.CPojoBase#invalidatePojo()
	 */
	@Override
	public void invalidatePojo() {
		// logs in the bundle output
		pBundleTracerLoggerService.logInfo(this, "invalidatePojo",
				"INVALIDATE", toDescription());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.psem2m.isolates.osgi.CPojoBase#validatePojo()
	 */
	@Override
	public void validatePojo() {
		// logs in the bundle output
		pBundleTracerLoggerService.logInfo(this, "validatePojo", "VALIDATE",
				toDescription());

		List<String> wIds = pLogChannelsService.getChannelsIds();

		pBundleTracerLoggerService.logInfo(this, null, "getChannelsIds=[%s]",
				CXStringUtils.stringListToString(wIds));
	}
}

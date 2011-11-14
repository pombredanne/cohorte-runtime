/**
 * File:   ErpCaller.java
 * Author: Thomas Calmant
 * Date:   14 nov. 2011
 */
package org.psem2m.composer.demo.impl.getItemsStock;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleException;
import org.psem2m.composer.demo.DemoComponentsConstants;
import org.psem2m.composer.test.api.IComponent;
import org.psem2m.demo.data.cache.ICachedObject;
import org.psem2m.isolates.base.IIsolateLoggerSvc;
import org.psem2m.isolates.base.activators.CPojoBase;

/**
 * getItemsStock result normalizer
 * 
 * @author Thomas Calmant
 */
@Component(name = DemoComponentsConstants.COMPONENT_NORMALIZER_GETITEMSSTOCK)
@Provides(specifications = IComponent.class)
public class NormalizerGetItemsStock extends CPojoBase implements IComponent {

    /** The instance name */
    @Property(name = DemoComponentsConstants.PROPERTY_INSTANCE_NAME)
    private String pInstanceName;

    /** The logger */
    @Requires
    private IIsolateLoggerSvc pLogger;

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.composer.test.api.IComponent#computeResult(java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> computeResult(final Map<String, Object> aData)
            throws Exception {

        final Object result = aData.get(KEY_RESULT);
        final Object error = aData.get(KEY_ERROR);

        if (error != null) {

            // Prepare a new map, with both result and error
            final Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(KEY_ERROR, error);
            resultMap.put(KEY_RESULT, result);

            return resultMap;
        }

        // Result is a CachedObject bean, transform it into a map
        if (result instanceof ICachedObject) {

            final ICachedObject<?> cachedObject = (ICachedObject<?>) result;
            final Map<String, Object> newResult = new HashMap<String, Object>();

            if (cachedObject.getObject() instanceof Map) {
                newResult
                        .putAll((Map<? extends String, ? extends Object>) cachedObject
                                .getObject());
            } else {
                newResult.put("content", cachedObject.getObject());
            }

            newResult.put("cacheAge", cachedObject.getCacheAge());
            return newResult;
        }

        // Result is a map, return it
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }

        // Unknown result type, return the whole data
        return aData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#invalidatePojo()
     */
    @Override
    @Invalidate
    public void invalidatePojo() throws BundleException {

        pLogger.logInfo(this, "invalidatePojo", "Component", pInstanceName,
                "Gone");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#validatePojo()
     */
    @Override
    @Validate
    public void validatePojo() throws BundleException {

        pLogger.logInfo(this, "validatePojo", "Component", pInstanceName,
                "Ready");
    }
}

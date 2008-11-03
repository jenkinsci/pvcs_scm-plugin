package hudson.plugins.pvcs_scm;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base test class.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public abstract class BaseTest extends TestCase
{
    /** Log4j Logger. */
    protected final Log logger = LogFactory.getLog(getClass());
}

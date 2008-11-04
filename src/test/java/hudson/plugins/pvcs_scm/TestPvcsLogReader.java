package hudson.plugins.pvcs_scm;

import java.util.Calendar;
import hudson.plugins.pvcs_scm.changelog.PvcsChangeLogSet;

/**
 * Test cases for PvcsLogReader.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public class TestPvcsLogReader extends BaseTest
{
    // {{{ testOne
    /**
     * Test successful log processing.
     */
    public void testOne() {
        Calendar lastBuildCal = Calendar.getInstance();
        lastBuildCal.set(2008, Calendar.SEPTEMBER, 1);
        
        PvcsLogReader reader = new PvcsLogReader(getClass().getResourceAsStream("testPvcsLog.log"),
                                                 "//repository/pvcs/repository/MYORG_MAINT",
                                                 "2008_10/",
                                                 lastBuildCal.getTime());
        
        reader.run();

        PvcsChangeLogSet changeSet = reader.getChangeLogSet();
        // logger.debug(changeSet.xmlText());
        assertTrue(changeSet.sizeOfEntryArray() > 0);
    }
    // }}}

    // {{{ testBadRecord
    /**
     * Tests the case where a new change log record starts before the old one
     * is closed out.
     */
    public void testBadRecord() {
        Calendar lastBuildCal = Calendar.getInstance();
        lastBuildCal.set(2008, Calendar.SEPTEMBER, 1);
        
        PvcsLogReader reader = new PvcsLogReader(getClass().getResourceAsStream("testPvcsLog_bad_record.log"),
                                                 "//repository/pvcs/repository/MYORG_MAINT",
                                                 "2008_10/",
                                                 lastBuildCal.getTime());
        
        reader.run();

        PvcsChangeLogSet changeSet = reader.getChangeLogSet();
        assertEquals("2008_10/MYORG-Java/aps/.project", changeSet.getEntryArray(0).getFileName());
    }
    // }}}

    // {{{ test3
    /**
     * 
     */
    public void test3() {
        Calendar lastBuildCal = Calendar.getInstance();
        lastBuildCal.set(2008, Calendar.OCTOBER, 16);
        
        PvcsLogReader reader = new PvcsLogReader(getClass().getResourceAsStream("testPvcsLog3.log"),
                                                 "//repository/pvcs/repository/MYORG_MAINT",
                                                 "2008_10/",
                                                 lastBuildCal.getTime());
        
        reader.run();

        PvcsChangeLogSet changeSet = reader.getChangeLogSet();
        logger.debug(changeSet.xmlText());

        assertEquals("2008_10/MYORG-Java/aps/src/main/java/com/abc/myorg/aps/bizobject/AddBankRequest.java", changeSet.getEntryArray(0).getFileName());
    }
    // }}}
    
}

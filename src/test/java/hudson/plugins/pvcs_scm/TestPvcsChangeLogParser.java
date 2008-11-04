package hudson.plugins.pvcs_scm;

import org.easymock.classextension.EasyMock;

import java.io.File;

import hudson.model.AbstractBuild;

/**
 * Test cases for PvcsChangeLogParser.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public class TestPvcsChangeLogParser extends BaseTest
{
    // {{{ testSuccessfulParse
    /**
     * Tests successful parsing of an XML file.
     */
    public void testSuccessfulParse() {
        PvcsChangeLogParser parser = new PvcsChangeLogParser();

        AbstractBuild mockBuild = EasyMock.createMock(AbstractBuild.class);

        EasyMock.replay(mockBuild);
        
        File changelogFile =
            new File(getClass().getResource("changelog.xml").getPath());
        
        PvcsChangeLogSet changeSet = null;
        
        try {
            changeSet = parser.parse(mockBuild, changelogFile);
        } catch (Exception e) {
            logger.error("got exception", e);
            fail("got exception");
        }

        EasyMock.verify(mockBuild);

        assertNotNull(changeSet);
        assertEquals(1, changeSet.getItems().length);
        assertEquals("MYORG-Java/pom.xml", changeSet.iterator().next().getAffectedPaths().iterator().next());
    }
    // }}}
    
}

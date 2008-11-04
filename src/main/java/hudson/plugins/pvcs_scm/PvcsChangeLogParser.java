package hudson.plugins.pvcs_scm;

import hudson.plugins.pvcs_scm.changelog.ChangeLogDocument;
import hudson.plugins.pvcs_scm.changelog.PvcsChangeLogEntry;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.text.SimpleDateFormat;

import org.xml.sax.SAXException;
import org.apache.xmlbeans.XmlException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Encapsulates the file format of the changelog.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public class PvcsChangeLogParser extends ChangeLogParser
{
    private Log logger = LogFactory.getLog(getClass());
    
    // {{{ parse
    /**
     * {@inheritDoc}
     */
    @Override
    public PvcsChangeLogSet parse(final AbstractBuild build,
                                  final File changelogFile)
        throws IOException, SAXException
    {
        PvcsChangeLogSet clSet = null;
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        
        logger.debug("parsing " + changelogFile);
        
        try {
            ChangeLogDocument doc = ChangeLogDocument.Factory.parse(changelogFile);

            clSet = new PvcsChangeLogSet(build);

            PvcsChangeLogSet.Entry clEntry;
            if (doc.getChangeLog().sizeOfEntryArray() > 0) {
                for (PvcsChangeLogEntry entry : doc.getChangeLog().getEntryArray()) {
                    clEntry = clSet.addNewEntry(Collections.singleton(entry.getFileName()),
                                                entry.getUserName(),
                                                entry.getComment());

                    clEntry.setRevision(entry.getRevision());
                    clEntry.setModifiedTime(df.format(entry.getModifiedTime().getTime()));
                }
            }
        } catch (XmlException e) {
            // @todo
            logger.error(String.format("Unable to parse %s: %s", changelogFile, e.getMessage()), e);
            throw new SAXException(e);
        }
    
        return clSet;
    }
    // }}}
    
}

package hudson.plugins.pvcs_scm;

import hudson.plugins.pvcs_scm.changelog.PvcsChangeLogSet;
import hudson.plugins.pvcs_scm.changelog.PvcsChangeLogEntry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generates a changelog set from the output of <code>pcli vlog</code>.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public class PvcsLogReader implements Runnable
{
    private final Log logger = LogFactory.getLog(getClass());
    
    private final String lineSep = System.getProperty("line.separator");

    private final SimpleDateFormat outDateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
    private final SimpleDateFormat outDateFormatSub = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

    /** Reader to read from. */
    private BufferedReader reader;

    /** 
     * Root of path where archive files are stored in the repository.  This is
     * not directly related to the "project root" (the exposed end-point of
     * the repository).
     */
    private String archiveRoot;

    /** . */
    private String pathPrefix;
    
    /** Archive file stuffix. */
    private String archiveFileSuffix = "_v";

    /** Reference time. */
    private Date lastBuild;
    
    private boolean firstRev = true;
    private boolean firstModifiedTime = true;
    private boolean firstUserName = true;
    private boolean nextLineIsComment = false;
    private boolean waitingForNextValidStart = false;

    private PvcsChangeLogSet changeLogSet;
    private PvcsChangeLogEntry modification;

    // {{{ constructor
    public PvcsLogReader(final InputStream is,
                         final String archiveRoot,
                         final String pathPrefix,
                         final Date lastBuild) 
    {
        this.reader = new BufferedReader(new InputStreamReader(is));
        this.archiveRoot = archiveRoot;
        this.pathPrefix = pathPrefix;
        this.lastBuild = lastBuild;

        changeLogSet = PvcsChangeLogSet.Factory.newInstance();
    }
    // }}}

    // {{{ getChangeLogSet
    public PvcsChangeLogSet getChangeLogSet() {
        return changeLogSet;
    }
    // }}}
    
    // {{{ run
    /**
     * 
     */
    public void run() {
        try {
            String s = reader.readLine();
            while (s != null) {
                consumeLine(s);
                s = reader.readLine();
            }
        } catch (IOException e) {
            // ignored
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }
    // }}}
    
    // {{{ consumeLine
    /**
     * 
     */
    private void consumeLine(final String line) {
        if (logger.isTraceEnabled()) {
            logger.trace("line: " + line);
        }
        
        if (line.startsWith("Archive:")) {
            if ((modification != null) & (! waitingForNextValidStart)) {
                // didn't find a valid entry before; delete this most recent
                // one

                logger.warn("discarding incomplete change log\n" + modification);
                
                changeLogSet.removeEntry(changeLogSet.sizeOfEntryArray() - 1);
            }
                    
            modification = changeLogSet.addNewEntry();
            
            firstModifiedTime = true;
            firstUserName = true;
            firstRev = true;
            
            nextLineIsComment = false;
            waitingForNextValidStart = false;

            int startIndex = line.indexOf(archiveRoot);
            if (startIndex != -1) {
                // found the branch in the archive path
                startIndex += archiveRoot.length();
            } else {
                startIndex = 0;
            }

            int endIndex = line.indexOf(archiveFileSuffix);
            if (endIndex == -1) {
                endIndex = line.length();
            }

            String fileName = line.substring(startIndex, endIndex);

            if (fileName.startsWith("/") || fileName.startsWith("\\")) {
                fileName = fileName.substring(1);
            }

            if (pathPrefix != null) {
                fileName = pathPrefix + fileName;
            }
            
            modification.setFileName(fileName);
        }
        else if (waitingForNextValidStart) {
            // we're in this state after we've got the last useful line
            // from the previous item, but haven't yet started a new one
            // -- we should just skip these lines till we start a new one
            // return
            // } else if (line.startsWith("Workfile:")) {
            // modification.createModifiedFile(line.substring(18), null);
        }
        // else if (line.startsWith("Archive created:")) {
        //     try {
        //         String createdDate = line.substring(18);
        // 
        //         Date createTime;
        //         try {
        //             createTime = outDateFormat.parse(createdDate);
        //         } catch (ParseException e) {
        //             createTime = outDateFormatSub.parse(createdDate);
        //         }
        // 
        //         ModifiedFile file = (ModifiedFile) modification.files.get(0);
        //         if (createTime.after(lastBuild)) {
        //             file.action = "added";
        //         } else {
        //             file.action = "modified";
        //         }
        //     } catch (ParseException e) {
        //         LOGGER.error("Error parsing create date: " + e.getMessage(), e);
        //     }
        // }
        else if (line.startsWith("Rev") && !line.startsWith("Rev count")) {
            if (firstRev) {
                firstRev = false;

                modification.setRevision(line.substring(4));
            }
        }
        else if (line.startsWith("Checked in:")) {
            /*
             * PVCS reports both "Checked in" and "Last modified"; I'm not
             * sure when "last modified" is updated, but I think "checked in"
             * is the best one to use for our purposes here.
             */

            // if this is the newest revision...
            if (firstModifiedTime) {
                firstModifiedTime = false;
                String lastMod = line.substring(16);
                Date modDate = null;
                
                try {
                    modDate = outDateFormat.parse(lastMod);
                } catch (ParseException e) {
                    logger.debug(String.format("Unable to parse modification time %s with %s",
                                              lastMod,
                                              outDateFormat.toPattern()));
                    
                    try {
                        modDate = outDateFormatSub.parse(lastMod);
                    } catch (ParseException pe) {
                        logger.error("Error parsing modification time " + lastMod + ": ", e);
                    }
                }

                Calendar modCal = Calendar.getInstance();
                modCal.setTime(modDate);
                modification.setModifiedTime(modCal);
            }
        }
        else if (nextLineIsComment) {
            // used boolean because don't know what comment will
            // startWith....
            boolean isDashesLine = line.equals("-----------------------------------");
            boolean isEqualsLine = line.equals("===================================");
            
            boolean isEndOfCommentsLine = isDashesLine || isEqualsLine;

            if (! (modification.getComment() != null) || (modification.getComment().length() == 0)) {
                modification.setComment(line);
            } else if (! isEndOfCommentsLine) {
                modification.setComment(modification.getComment() + lineSep + line);
            } else {
                // then set indicator to ignore future lines till next new
                // item
                waitingForNextValidStart = true;
            }
        }
        else if (line.startsWith("Author id:")) {
            // if this is the newest revision...
            if (firstUserName) {
                StringTokenizer st = new StringTokenizer(line.substring(11), " ");

                modification.setUserName(st.nextToken().trim());

                firstUserName = false;
                nextLineIsComment = true;
            }
        } // end of Author id
    }
    // }}}
}

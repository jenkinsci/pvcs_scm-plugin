package hudson.plugins.pvcs_scm;

import hudson.scm.ChangeLogSet;
import hudson.model.User;
import hudson.model.AbstractBuild;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a set of changelog entries for a given build.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public class PvcsChangeLogSet extends ChangeLogSet<PvcsChangeLogSet.Entry>
{
    private List<PvcsChangeLogSet.Entry> entries;
    
    public static class Entry extends ChangeLogSet.Entry 
    {
        private Collection<String> affectedPaths;
        private String user;
        private String msg;
        private String revision;
        private String modifiedTime;
        
        // {{{ constructor
        public Entry(final ChangeLogSet parent,
                     final Collection<String> affectedPaths,
                     final String user,
                     final String msg) 
        {
            super();
            
            setParent(parent);
            
            this.affectedPaths = affectedPaths;
            this.user = user;
            this.msg = msg;
        }
        // }}}

        // {{{ getAffectedPaths
        /**
         * {@inheritDoc}
         */
        public Collection<String> getAffectedPaths() {
            return affectedPaths;
        }
        // }}}

        // {{{ getUser
        /**
         * {@inheritDoc}
         */
        public User getAuthor() {
            return User.get(user);
        }
        // }}}

        // {{{ getMsg
        /**
         * {@inheritDoc}
         */
        public String getMsg() {
            return msg;
        }
        // }}}

        // {{{ getRevision
        public String getRevision() {
            return revision;
        }
        // }}}
        
        // {{{ setRevision
        public void setRevision(final String revision) {
            this.revision = revision;
        }
        // }}}
        
        // {{{ getModifiedTime
        public String getModifiedTime() {
            return modifiedTime;
        }
        // }}}
        
        // {{{ setModifiedTime
        public void setModifiedTime(final String modifiedTime) {
            this.modifiedTime = modifiedTime;
        }
        // }}}
    }
    
    // {{{ constructor
    /**
     * 
     */
    protected PvcsChangeLogSet(final AbstractBuild build) {
        super(build);

        entries = new ArrayList<PvcsChangeLogSet.Entry>();
    }
    // }}}

    // {{{ isEmptySet
    /**
     * 
     */
    public boolean isEmptySet() {
        return entries.isEmpty();
    }
    // }}}

    // {{{ iterator
    /**
     * {@inheritDoc}
     */
    public Iterator<PvcsChangeLogSet.Entry> iterator() {
        return entries.iterator();
    }
    // }}}
    
    // {{{ addEntry
    /**
     * Adds an entry to the change set.
     */
    public void addEntry(final Collection<String> affectedPaths,
                         final String user,
                         final String msg)
    {
        entries.add(addNewEntry(affectedPaths, user, msg));
    }
    // }}}

    // {{{ addNewEntry
    /**
     * Returns a new Entry, which is already added to the list.
     *
     * @return new Entry instance
     */
    public Entry addNewEntry(final Collection<String> affectedPaths,
                             final String user,
                             final String msg)
    {
        Entry entry = new Entry(this, affectedPaths, user, msg);
        entries.add(entry);
        return entry;
    }
    // }}}
    
}

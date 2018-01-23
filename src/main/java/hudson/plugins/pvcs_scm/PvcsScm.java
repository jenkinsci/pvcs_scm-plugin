package hudson.plugins.pvcs_scm;

import hudson.Extension;
import hudson.plugins.pvcs_scm.changelog.ChangeLogDocument;
import hudson.plugins.pvcs_scm.changelog.PvcsChangeLogSet;
import hudson.plugins.pvcs_scm.changelog.PvcsChangeLogEntry;

import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.FilePath;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.Run;

import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;

import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.QueryParameter;

/**
 * Provides integration with PCVS.
 *
 * @author Brian Lalor &lt;blalor@bravo5.org&gt;
 */
public class PvcsScm extends SCM
{
    private static final Log LOGGER = LogFactory.getLog(PvcsScm.class);

    /**
     * Date format required by commands passed to PVCS
     */
    private static final String IN_DATE_FORMAT = "MM/dd/yyyy hh:mm:ss aa";

    /**
     * Date format returned in the output of PVCS commands.
     */
    private static final String OUT_DATE_FORMAT = "MMM dd yyyy HH:mm:ss";

    private String projectRoot;
    private String archiveRoot;

    /**
     * Additional prefix to changeset filenames to match with what {@link hudson.maven.FilteredChangeLogSet}
     * expects.
     */
    private String changeLogPrefixFudge;

    private String moduleDir;
    private String loginId;
    private String pvcsWorkspace;
    private String promotionGroup;
    private String versionLabel;
    private boolean cleanCopy;
    //private int MAX;
    // {{{ constructor
    @DataBoundConstructor
    public PvcsScm(final String projectRoot,
                   final String archiveRoot,
                   final String changeLogPrefixFudge,
                   //final int MAX,
                   final String moduleDir,
                   final String loginId,
                   final String pvcsWorkspace,
                   final String promotionGroup,
                   final String versionLabel,
                   final boolean cleanCopy) 
    {
        this.projectRoot = projectRoot;
        this.archiveRoot = archiveRoot;
        this.changeLogPrefixFudge = changeLogPrefixFudge;
        this.moduleDir= moduleDir;
        this.loginId = loginId;
        this.pvcsWorkspace = pvcsWorkspace;
        this.promotionGroup = promotionGroup;
        this.versionLabel = versionLabel;
        this.cleanCopy = cleanCopy;
        LOGGER.debug("created new instance");
    }
    // }}}

    // {{{ getProjectRoot
    public String getProjectRoot() {
        return projectRoot;
    }
    // }}}
    
    // {{{ setProjectRoot
    public void setProjectRoot(final String projectRoot) {
        this.projectRoot = projectRoot;
    }
    // }}}

    // {{{ getArchiveRoot
    public String getArchiveRoot() {
        return archiveRoot;
    }
    // }}}
    
    // {{{ setArchiveRoot
    public void setArchiveRoot(final String archiveRoot) {
        this.archiveRoot = archiveRoot;
    }
    // }}}
    
    // {{{ isCleanCopy
    public boolean isCleanCopy() {
        return cleanCopy;
    }
    // }}}
    
    // {{{ setCleanCopy
    public void setCleanCopy(final boolean cleanCopy) {
        this.cleanCopy = cleanCopy;
    }
    // }}}
    
    // {{{ getModuleDir
    public String getModuleDir() {
        return this.moduleDir;
    }
    // }}}
    
    // {{{ setModuleDir
    public void setModuleDir(final String moduleDir) {
        System.out.println("moduleDir");
        this.moduleDir=moduleDir;
    }
    // }}}

    // {{{ getLoginId
    public String getLoginId() {
        return loginId;
    }
    // }}}

    // {{{ setLoginId
    public void setLoginId(final String loginId) {
        this.loginId = loginId;
    }
    // }}}

    // {{{ getPvcsWorkspace
    public String getPvcsWorkspace() {
        return pvcsWorkspace;
    }
    // }}}

    // {{{ setPvcsWorkspace
    public void setPvcsWorkpace(final String pvcsWorkspace) {
        this.pvcsWorkspace = pvcsWorkspace;
    }
    // }}}

    // {{{ getPromotionGroup
    public String getPromotionGroup() {
        return promotionGroup;
    }
    // }}}

    // {{{ setPromotionGroup
    public void setPromotionGroup(final String promotionGroup) {
        this.promotionGroup = promotionGroup;
    }
    // }}}

    // {{{ getVersionLabel
    public String getVersionLabel() {
        return versionLabel;
    }
    // }}}

    // {{{ setVersionLabel
    public void setVersionLabel(final String versionLabel) {
        this.versionLabel = versionLabel;
    }
    // }}}

    // {{{ getChangeLogPrefixFudge
    public String getChangeLogPrefixFudge() {
        return changeLogPrefixFudge;
    }
    // }}}
    
    // {{{ setChangeLogPrefixFudge
    public void setChangeLogPrefixFudge(final String changeLogPrefixFudge) {
        this.changeLogPrefixFudge = changeLogPrefixFudge;
    }
    // }}}

    // {{{ requiresWorkspaceForPolling
    /**
     * TODO
     */
    @Override
    public boolean requiresWorkspaceForPolling() {
        return false;
    }
    // }}}
    
    // {{{ checkout
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkout(final AbstractBuild build,
                            final Launcher launcher,
                            final FilePath workspace,
                            final BuildListener listener,
                            final File changelogFile)
        throws IOException, InterruptedException
    {
        LOGGER.trace("in checkout()");
        
        boolean checkoutSucceeded = true;

        ChangeLogDocument doc = ChangeLogDocument.Factory.newInstance();

        Run previousBuild = build.getPreviousBuild();
        if (previousBuild != null) {
            doc.setChangeLog(getModifications(launcher,
                                              listener,
                                              previousBuild.getTimestamp()));

            doc.getChangeLog().setLastBuildId(previousBuild.getId());
            doc.getChangeLog().setLastBuildTime(previousBuild.getTimestamp());
        } else {
            doc.addNewChangeLog();
        }
            
        doc.getChangeLog().setBuildId(build.getId());
        doc.save(changelogFile);

        if (cleanCopy) {
            listener.getLogger().println("clean copy configured; deleting contents of " + workspace);

            LOGGER.info("deleting contents of workspace " + workspace);
            
            workspace.deleteContents();
        }
        
        
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(getDescriptor().getExecutable());
        cmd.add("-nb", "run", "-ns", "-y");
        cmd.add("get");
        cmd.add("-pr" + projectRoot);
        if (loginId != null && !loginId.trim().equals("")) {
            cmd.add("-id" + loginId);
        }
        if (pvcsWorkspace != null && !pvcsWorkspace.trim().equals("")) {
            cmd.add("-sp" + pvcsWorkspace);
        }
        if (versionLabel != null && !versionLabel.trim().equals("")) {
            cmd.add("-v" + versionLabel);
        }else if (promotionGroup != null && !promotionGroup.trim().equals("")) {
            cmd.add("-g" + promotionGroup);
        }

        cmd.add("-bp/");
        cmd.add("-o");
        cmd.add("-a.");
        cmd.add("-z");
        cmd.add(moduleDir);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LOGGER.debug("launching command " + cmd.toList());

        int rc = launcher.launch().cmds(cmd).stdout(baos).pwd(workspace).join();

        if (rc != 0) {
            // checkoutSucceeded = false;

            LOGGER.error("command exited with " + rc);
            listener.error("command exited with " + rc);
            // listener.error(baos.toString());
            listener.error("continuing anyway.  @todo: filter results from PVCS");

        } /* else */ {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("pcli output:\n" + new String(baos.toByteArray()));
            }

            listener.getLogger().println("pcli output:");
            listener.getLogger().write(baos.toByteArray(), 0, baos.size());

        }
       
    
         return checkoutSucceeded;
    }
    // }}}

    // {{{ pollChanges
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pollChanges(final AbstractProject project,
                               final Launcher launcher,
                               final FilePath workspace,
                               final TaskListener listener)
        throws IOException, InterruptedException
    {
        LOGGER.debug("polling for changes in " + workspace);
        
        // default to change being detected
        boolean changeDetected = true;
        
        if (project.getLastBuild() == null) {
            LOGGER.info("no existing build; starting a new one");
            listener.getLogger().println("no existing build; starting a new one");
        } else {
            PvcsChangeLogSet changeSet =
                getModifications(launcher,
                                 listener,
                                 project.getLastBuild().getTimestamp());


            changeDetected = (changeSet.sizeOfEntryArray() > 0);

            if (! changeDetected) {
                listener.getLogger().println("no changes detected");
            } else {
                for (PvcsChangeLogEntry entry : changeSet.getEntryArray()) {
                    listener.getLogger().print("==> " + entry.getFileName() + " ");
                    listener.getLogger().print(entry.getRevision() + " ");
                    listener.getLogger().print(entry.getModifiedTime().getTime() + " ");
                    listener.getLogger().println(entry.getModifiedTime().getTime());
                    listener.getLogger().println(entry.getComment());
                }
            }
        }
        
        return changeDetected;
    }
     // }}}

    // {{{ getModifications
    /**
     * Returns a PvcsChangeLogSet containing all change entries since
     * lastBuild.
     *
     * @param launcher the launcher to use to invoke the PVCS client.
     * @param listener task listener for outputting status
     * @param lastBuild the last time the job was built.
     */
    public PvcsChangeLogSet getModifications(final Launcher launcher,
                                             final TaskListener listener,
                                             final Calendar lastBuild)
        throws IOException, InterruptedException
    {
        Calendar now = Calendar.getInstance();

        LOGGER.info("looking for changes between " + lastBuild.getTime() + " and " + now.getTime());
        listener.getLogger().println("looking for changes between " + lastBuild.getTime() + " and " + now.getTime());

        SimpleDateFormat df = new SimpleDateFormat(IN_DATE_FORMAT);
        
        PipedOutputStream os = new PipedOutputStream();

        PvcsLogReader logReader =
            new PvcsLogReader(new PipedInputStream(os),
                              archiveRoot,
                              changeLogPrefixFudge,
                              lastBuild.getTime());
        
        
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(getDescriptor().getExecutable());
        cmd.add("-nb", "run", "-ns", "-q");
        cmd.add("vlog");
        cmd.add("-pr" + projectRoot);
        if (loginId != null && !loginId.trim().equals("")) {
            cmd.add("-id" + loginId);
        }
        if (pvcsWorkspace != null && !pvcsWorkspace.trim().equals("")) {
            cmd.add("-sp" + pvcsWorkspace);
        }
        if (versionLabel != null && !versionLabel.trim().equals("")) {
            cmd.add("-v" + versionLabel);
        }
        if (promotionGroup != null && !promotionGroup.trim().equals("")) {
            cmd.add("-g" + promotionGroup);
        }
        cmd.add("-i");
        cmd.add("-ds" + df.format(lastBuild.getTime()));
        cmd.add("-de" + df.format(now.getTime()));
        cmd.add("-z");
        cmd.add(moduleDir);



        LOGGER.debug("launching command " + cmd.toList());

        Proc proc = launcher.launch().cmds(cmd).stdout(os).start();

        Thread t = new Thread(logReader);
        t.start();

        int rc = proc.join();
        os.close();

        t.join();

        if (rc != 0) {
            LOGGER.error("command failed, returned " + rc);
            listener.error("command failed, returned " + rc);
        }
        


        return logReader.getChangeLogSet();
    }
    // }}}

    // {{{ createChangeLogParser
    /**
     * {@inheritDoc}
     */
    @Override
    public PvcsChangeLogParser createChangeLogParser() {
        return new PvcsChangeLogParser();
    }
    // }}}

    // {{{ getDescriptor
    /**
     *
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }
    // }}}

    public static final class DescriptorImpl extends SCMDescriptor<PvcsScm>
    {
        private static final Log LOGGER = LogFactory.getLog(DescriptorImpl.class);

        @Extension
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private String executable = "pcli";

        // {{{ constructor
        private DescriptorImpl() {
            super(PvcsScm.class, null);
            load();
        }
        // }}}

        // {{{ getDisplayName
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "PVCS";
        }
        // }}}

        // {{{ configure
        /**
         *
         */
        @Override
        public boolean configure(final StaplerRequest req, JSONObject formData) throws FormException {
            LOGGER.debug("configuring from " + req);

            executable = Util.fixEmpty(req.getParameter("pvcs.executable").trim());
            save();
            return true;
        }
        // }}}

        // {{{ getExecutable
        public String getExecutable() {
            return executable;
        }
        // }}}

        // {{{ doExecutableCheck
        /**
         *
         */
        public FormValidation doExecutableCheck(@QueryParameter String value) {
            return FormValidation.validateExecutable(value);
        }
        // }}}
    }
}

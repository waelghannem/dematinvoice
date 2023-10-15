package com.demat.invoice.utils;

import static java.lang.String.format;
import org.apache.logging.log4j.Level;
import org.apache.logging.slf4j.SLF4JLogger;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.slf4j.spi.LocationAwareLogger;

import java.io.File;
import java.io.PrintStream;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS;
import static org.apache.logging.log4j.Level.*;
import static org.apache.tools.ant.Project.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.slf4j.spi.LocationAwareLogger.*;

/**
 * @author Romain ROSSI
 * @company Byzaneo
 * @date 2006-11-20 15:19:15
 * @since 1.1
 */
public class AntHelper {
  private static final LocationAwareLogger log = (LocationAwareLogger) getLogger(AntHelper.class);

  public static Project project = null; // NOSONAR : can't be final because there is initialization

  /**
   * private constructor to hide the implicit public one
   */
  private AntHelper() {
  }

  public static final Project createProject() {
    if (project == null)
      project = createProject(
          log.isTraceEnabled() ? TRACE
              : log.isDebugEnabled() ? DEBUG
                  : log.isInfoEnabled() ? INFO
                      : log.isWarnEnabled() ? WARN
                          : ERROR);
    return project;

  }

  public static final Project createProject(final Level logLevel) {

    project = new Project();

    project.addReference("ant.projectHelper", ProjectHelper.getProjectHelper());
    project.init();
    project.getBaseDir();
    project.addBuildListener(new BuildLogger() {
      private long startTime = currentTimeMillis();

      @Override
      public void taskStarted(BuildEvent event) {
        if (event.getTask() != null)
          log(event, "{} started", event.getTask()
              .getTaskName());
      }

      @Override
      public void taskFinished(BuildEvent event) {
        if (event.getTask() != null)
          log(event, "{} finished", event.getTask()
              .getTaskName());
      }

      @Override
      public void targetStarted(BuildEvent event) {
        if (event.getTarget() != null)
          log(event, "{} started", event.getTarget()
              .getName());
      }

      @Override
      public void targetFinished(BuildEvent event) {
        if (event.getTarget() != null)
          log(event, "{} finished", event.getTarget()
              .getName());
      }

      @Override
      public void messageLogged(BuildEvent event) {
        log(event, event.getMessage());
      }

      @Override
      public void buildStarted(BuildEvent event) {
        startTime = System.currentTimeMillis();
      }

      @Override
      public void buildFinished(BuildEvent event) {
        log(event, "{} action in {}",
            event.getException() == null ? "Successfull" : "Failed",
            formatDurationHMS(currentTimeMillis() - startTime));
      }

      @Override
      public void setOutputPrintStream(PrintStream output) {
        /* Not Implemented */ }

      @Override
      public void setMessageOutputLevel(int level) {
        /* Not Implemented */ }

      @Override
      public void setErrorPrintStream(PrintStream err) {
        /* Not Implemented */ }

      @Override
      public void setEmacsMode(boolean emacsMode) {
        /* Not Implemented */ }

      private void log(BuildEvent event, String message, Object... args) {
        int ilevel;
        Level level;
        switch (event.getPriority()) {
        case MSG_ERR:
          ilevel = ERROR_INT;
          level = ERROR;
          break;
        case MSG_WARN:
          ilevel = WARN_INT;
          level = WARN;
          break;
        case MSG_INFO:
          ilevel = INFO_INT;
          level = INFO;
          break;
        case MSG_DEBUG:
        case MSG_VERBOSE:
          ilevel = DEBUG_INT;
          level = DEBUG;
          break;
        // case MSG_VERBOSE: level = TRACE_INT; break
        default:
          ilevel = INFO_INT;
          level = INFO;
        }
        if (level.isMoreSpecificThan(logLevel))
          log.log(null, SLF4JLogger.class.getName(), ilevel,
              event.getTask() != null ? format("[%s] %s", event.getTask()
                  .getTaskName(), message) : message,
              args, event.getException());
      }
    });

    return project;
  }

  /**
   * File count selector to limit the number of file processed (copied or moved). Only the files are counted (not the directories). If the
   * count value is lower than 0, this selector is disabled. If the count is 0, no file will be processed.
   */
  public static final class FileCountSelector implements FileSelector {
    private int count;
    private final boolean enabled;

    public FileCountSelector(int count) {
      this.count = count;
      this.enabled = count >= 0;
    }

    @Override
    public boolean isSelected(File basedir, String filename, File file) throws BuildException {
      return !enabled || file.isDirectory() || 0 < (count--);
    }
  }

}

package com.demat.invoice.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tools.ant.*;
import org.apache.tools.ant.filters.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.types.selectors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.demat.invoice.beans.FileType.UNKNOWN;
import static com.demat.invoice.utils.AntHelper.createProject;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.*;
import static java.util.Arrays.sort;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.zip.GZIPInputStream.GZIP_MAGIC;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.io.IOUtils.*;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang3.time.DateFormatUtils.format;
import static org.apache.tika.io.TikaInputStream.get;

/**
 * @author Loic ABEMONTY
 * @author Romain ROSSI
 * @company Byzaneo
 */
public class FileHelper extends FileUtils {
  private static final Logger log = LoggerFactory.getLogger(FileHelper.class);

  private static final String READ_RESULT_PROPERTY = "filtered_read_result";

  /** Mime type detection based on Tika */
  private static TikaConfig tika;

  /*
   * ZIP
   */

  public static final boolean unzip(File zip, File destination, String includePattern) {
    if (zip == null || destination == null) {
      log.error("Impossible to extract " + zip + " archive to: " + destination);
      return false;
    }

    Expand expand = new Expand();
    expand.setProject(createProject());
    expand.setTaskName("expand:" + zip.getName());
    expand.setSrc(zip);
    expand.setOverwrite(true);
    expand.setDest(destination);

    if (includePattern != null) {
      PatternSet pattern = new PatternSet();
      pattern.setProject(expand.getProject());
      pattern.setIncludes(includePattern);
      expand.addPatternset(pattern);
    }

    try {
      expand.execute();
    }
    catch (BuildException be) {
      log.error("Error during " + zip + " extraction into " + destination, be);
      return false;
    }

    return true;
  }

  public static boolean zip(File dest, File source, String prefix) {
    if (dest == null || source == null) {
      log.error("Impossible to create " + dest + " archive from: " + source);
      return false;
    }

    Zip zip = new Zip();
    zip.setProject(createProject());
    zip.setTaskName("zip:" + dest.getName());
    zip.setDestFile(dest);

    ZipFileSet zfs = new ZipFileSet();
    zfs.setProject(zip.getProject());
    zfs.setDir(source);
    if (prefix != null) {
      zfs.setPrefix(prefix);
    }

    zip.addZipfileset(zfs);

    try {
      zip.execute();
    }
    catch (BuildException be) {
      log.error("Error during " + dest + " compression from " + source, be);
      return false;
    }

    return true;
  }

  /**
   * @param files to archive
   * @param zipout output stream
   * @since 3.2
   */
  public static final void zip(final Collection<File> files, OutputStream zipout) {
    if (isEmpty(files) || zipout == null)
      return;
    try (final ZipOutputStream zos = new ZipOutputStream(zipout)) {
      for (File file : files) {
        if (file == null || !file.isFile())
          continue;
        ZipEntry entry = new ZipEntry(file.getName());
        zos.putNextEntry(entry);
        try (final RandomAccessFile raf = new RandomAccessFile(file, "r");
            final FileChannel inChannel = raf.getChannel();) { // NOSONAR : double try-catch
          MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
          buffer.load();
          for (int i = 0; i < buffer.limit(); i++) { // NOSONAR : function too deeply
            zos.write(buffer.get());
          }
          buffer.clear();
        }
        catch (Exception e) {
          log.debug("Error getting zip() : {}", getRootCauseMessage(e));
          log.error("Error writing zip entry: " + file, e);
        }
        zos.closeEntry();
      }
    }
    catch (IOException ioe) {
      log.error("Error creating zip", ioe);
    }
  }

  public static boolean zipFile(File sourceToZip, Path destination) {

    if (!sourceToZip.isFile()) {
      log.error("Cannot zip a folder: %s", sourceToZip.toString());
      return false;
    }

    try (FileOutputStream fos = new FileOutputStream(destination.toString());
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        FileInputStream fis = new FileInputStream(sourceToZip)) {

      ZipEntry zipEntry = new ZipEntry(sourceToZip.getName());
      zipOut.putNextEntry(zipEntry);
      final byte[] bytes = new byte[1024];
      int length;
      while ((length = fis.read(bytes)) >= 0) {
        zipOut.write(bytes, 0, length);
      }

    }
    catch (IOException e) {
      log.error("Error during zipping the folder: {} : {}", sourceToZip.toString(), e.getMessage());
      return false;
    }

    return true;
  }

  /** ZIP Magic mark */
  private static final byte[] ZIP_MAGIC = { 'P', 'K', 0x3, 0x4 };

  /**
   * Tests if the given data is a ZIP archive.
   *
   * @param data the byte array to test.
   * @return true if the stream is a ZIP archive.
   * @since 3.1 COM-125
   */
  public static final boolean isZip(byte[] data) {
    try (final ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
      return isZip(stream);
    }
    catch (IOException e) {
      log.debug("Error getting isZip() : {}", getRootCauseMessage(e));
      return false;
    }
  }

  /**
   * Tests if the given input stream is a ZIP archive.
   *
   * @param stream the input stream to test. The stream is buffered if necessary and returned reseted but not closed.
   * @return true if the stream is a ZIP archive.
   * @since 3.1 COM-125
   */
  public static final boolean isZip(InputStream stream) {
    if (!stream.markSupported()) {
      stream = new BufferedInputStream(stream); // NOSONAR : variable reassigned
    }
    boolean isZip;
    try {
      byte[] buffer = new byte[ZIP_MAGIC.length];
      stream.mark(ZIP_MAGIC.length);
      int read = stream.read(buffer);
      isZip = read != -1 && Arrays.equals(ZIP_MAGIC, buffer);
      stream.reset();
    }
    catch (IOException e) {
      log.debug("Error getting isZip() : {}", getRootCauseMessage(e));
      isZip = false;
    }
    return isZip;
  }

  /**
   * Tests if a file is a ZIP file.
   *
   * @param f the file to test.
   * @return true if the file is a ZIP archive.
   * @since 3.1 COM-125
   */
  public static final boolean isZip(final File f) {
    boolean isZip = true;
    final byte[] buffer = new byte[ZIP_MAGIC.length];
    try (final RandomAccessFile raf = new RandomAccessFile(f, "r")) {
      raf.readFully(buffer);
      for (int i = 0; i < ZIP_MAGIC.length; i++) {
        if (buffer[i] != ZIP_MAGIC[i]) {
          isZip = false;
          break;
        }
      }
    }
    catch (Exception e) {
      log.debug("Error getting isZip() : {}", getRootCauseMessage(e));
      isZip = false;
    }
    return isZip;
  }

  /**
   * Checks if an input stream is a GZIP.
   *
   * @param in the input stream to test. The stream is buffered if necessary and returned reseted but not closed.
   * @return true if the stream is a GZIP archive.
   * @since 3.1 COM-125
   */
  public static final boolean isGZip(InputStream in) {
    if (!in.markSupported()) {
      in = new BufferedInputStream(in); // NOSONAR : variable reassigned
    }
    in.mark(2);
    try {
      int magic = in.read() & 0xff | ((in.read() << 8) & 0xff00);
      in.reset();
      return magic == GZIP_MAGIC;
    }
    catch (IOException e) {
      log.error("Error checking GZip format on stream", e);
      return false;
    }
  }

  /**
   * Checks if a file is a GZIP.
   *
   * @param file to test.
   * @return true if the file is a GZIP archive.
   * @since 3.1 COM-125
   */
  public static final boolean isGZip(final File file) {
    try (final RandomAccessFile raf = new RandomAccessFile(file, "r")) {
      return (raf.read() & 0xff | ((raf.read() << 8) & 0xff00)) == GZIP_MAGIC;
    }
    catch (Exception e) {
      log.debug("Error getting isGZip() : {}", getRootCauseMessage(e));
      return false;
    }
  }

  /*
   * FILESET
   */

  public static final String[] fileSet(final File dir, final String includes) {
    return fileSet(dir, includes, true);
  }

  public static final String[] fileSet(final File dir, final String includes, final boolean isCaseSensitive) {
    FileSet fs = new FileSet();
    fs.setProject(createProject());
    fs.setDir(dir);
    fs.setCaseSensitive(isCaseSensitive);
    if (includes != null)
      fs.setIncludes(includes);
    DirectoryScanner ds = fs.getDirectoryScanner(fs.getProject());
    return ds.getIncludedFiles();
  }


  /**
   * @since COM-92
   */
  public static final FileSet createFileSet(final File dir,
      final String includes, final String excludes, final boolean caseSensitive,
      final String containRegExp, final String contain, final int latency) {

    FileSet fileSet = new FileSet();
    fileSet.setProject(createProject());
    fileSet.setDir(dir);
    fileSet.setIncludes(includes);
    fileSet.setExcludes(excludes);
    fileSet.setCaseSensitive(caseSensitive);

    if (StringHelper.isNotBlank(containRegExp)) {
      ContainsRegexpSelector ces = new ContainsRegexpSelector();
      ces.setExpression(containRegExp);
      fileSet.appendSelector(ces);
    }
    else if (StringHelper.isNotBlank(contain)) {
      AndSelector andSelect = new AndSelector();
      String[] stoken = StringHelper.split(contain);
      for (String s : stoken) {
        ContainsSelector cs = new ContainsSelector();
        cs.setText(s);
        cs.setCasesensitive(true);
        andSelect.addContains(cs);
      }
      fileSet.addAnd(andSelect);
    }
    long beforeTreatedMillis = getCurrentTimeMillis() - (latency * 1000);
    if (latency > 0) {
      // Since AIO-7132
      // if the difference between the current date and the last modification of the file is less than the provided latency do not treat the
      // file
      DateSelector selector = new DateSelector();
      selector.setMillis(beforeTreatedMillis);
      selector.setWhen(TimeComparison.BEFORE);
      fileSet.addDate(selector);

    }
    // trace the file set only if debug is enabled
    if (log.isDebugEnabled()) {
      StringBuilder trace = new StringBuilder("File set: ").append(dir)
          .append("/")
          .append(includes);
      if (StringHelper.isNotBlank(containRegExp)) {
        trace.append("(")
            .append(containRegExp)
            .append(")");
      }
      if (StringHelper.isNotBlank(contain)) {
        trace.append("(")
            .append(contain)
            .append(")");
      }
      if (latency > 0) {
        trace.append("(last modified before ")
            .append(format(beforeTreatedMillis, "yyyy-MM-dd HH:mm:ss"))
            .append(")");
      }
      log.debug(trace.toString());
    }

    return fileSet;
  }

  /**
   * Returns the current time in milliseconds (extracted to method so it can be mocked in unit tests)
   *
   * @return
   */
  public static long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  /**
   * @since COM-92
   * @param dir
   * @param includes
   * @param excludes
   * @param caseSensitive
   * @param containRegExp
   * @param contain
   * @return
   */
  public static final Collection<File> fileSet(final File dir,
      final String includes, final String excludes, final boolean caseSensitive,
      final String containRegExp, final String contain) {
    return fileSet(dir, createFileSet(dir, includes, excludes, caseSensitive, containRegExp, contain, 0));
  }

  /**
   * @param dir
   * @param fileSet
   * @return
   */
  public static final Collection<File> fileSet(final File dir, final FileSet fileSet) {
    if (dir == null || !dir.isDirectory() || fileSet == null)
      return emptyList();

    final DirectoryScanner ds = fileSet.getDirectoryScanner(fileSet.getProject());
    final String[] relPathes = ds.getIncludedFiles();
    if (relPathes == null)
      return emptyList();

    return stream(relPathes)
        .map(relPath -> new File(dir, relPath))
        .collect(toList());
  }

  /**
   * Return the files(no directories) from a <code>directory</code>. The files can be filtered by <code>includes</code>(regex) and
   * <code>excludes</code>(regex).
   * <p>
   * <b> Note: if no rules are needed then set <code>includes</code> and <code>excludes</code> to null. </b>
   * </p>
   *
   * @param directory - a directory to search in
   * @param include - regex expression for which files to include
   * @param excludes - regex expressions for which files to exclude separated by space or comma
   * @return a {@link Collection} of {@link File Files}
   */
  public static final Collection<File> getFilesFromDirectoryUsingRegex(File directory, String include, String excludes) {
    if (directory == null || !directory.isDirectory()) {
      log.error("The file is not a valid directory");
      return emptyList();
    }
    FileSet fs = new FileSet();
    fs.setProject(createProject());
    // in case include contains space or comma, which are separators in FileSet
    fs.appendIncludes(new String[] { include });
    fs.setExcludes(excludes);
    fs.setDir(directory);
    DirectoryScanner ds = fs.getDirectoryScanner(fs.getProject());
    String[] filesFound = ds.getIncludedFiles();
    if (filesFound == null) {
      return emptyList();
    }
    return stream(filesFound)
        .map(relPath -> new File(directory, relPath))
        .collect(toList());
  }

  /**
   * getListByAscendingDate
   *
   * @param folder
   * @return
   */
  public static File[] getListByAscendingDate(File folder) {
    if (!folder.isDirectory()) {
      File returnNull = new File("Null");
      return returnNull.listFiles();
    }
    File files[] = folder.listFiles();
    sort(files, Comparator.comparing(File::lastModified));
    return files;
  }

  /**
   * getListByAscendingDate
   *
   * @param folder
   * @return list file order by date Descending
   */

  public static File[] getListByDescendingDate(File folder) {
    if (!folder.isDirectory()) {
      File returnNull = new File("Null");
      return returnNull.listFiles();
    }
    File files[] = folder.listFiles();
    sort(files, Comparator.comparing(File::lastModified)
        .reversed());
    return files;
  }

  /**
   * Uses Ant's {@link Delete} task to remove the given file or directory. The file or the directory will be set to be removed on JVM exit
   * if the runtime deletion failed.
   *
   * @see Delete
   * @param file
   * @return false if the file can not be delete or if the file doesn't exist.
   */
  public static boolean deleteFile(File file) {
    return deleteFile(file, true);
  }

  /**
   * Uses Ant's {@link Delete} task to remove the given file or directory. The file or the directory will be set to be removed on JVM exit
   * if the runtime deletion failed.
   *
   * @see Delete
   * @param file
   * @param verbose : if true, list all names of deleted files.
   * @return false if the file can not be delete or if the file doesn't exist.
   */
  public static boolean deleteFile(File file, boolean verbose) {
    if (file == null)
      return false;
    if (!file.exists())
      return true;

    Delete del = new Delete();
    del.setProject(createProject());
    del.setTaskName("delete:" + file.getName());
    if (file.isFile()) {
      del.setFile(file);
    }
    else {
      del.setDir(file);
      del.setIncludeEmptyDirs(true);
    }

    if (log.isDebugEnabled() && verbose)
      del.setVerbose(true);

    try {
      del.execute();
    }
    catch (BuildException be) {
      log.error("Error during " + file + " suppression. We'll try to delete the file on JVM exit", be);
      del.setDeleteOnExit(true);
      try {
        del.execute();
      }
      catch (BuildException be2) {
        log.debug("Error getting delete() : {}", getRootCauseMessage(be2));
        log.error("Error during " + file + " suppression (even on JVM exit)", be);
      }
      return false;
    }

    return true;
  }

  /*
   * URL & PATH
   */

  public static final URL resolveURL(File file) {
    if (file == null)
      return null;
    return resolveURL(file.getAbsolutePath());
  }

  public static final URL resolveURL(String path) {
    File file = new File(path);
    try {
      if (file.exists()) {
        return file.toURI()
            .toURL();
      }
      path.replace('\\', '/');
      path.replaceAll("//", "/");
      if (path.startsWith("/")) {
        path = "file:" + path; // NOSONAR : variable reassigned
      }
      else if (path.startsWith("file:")) {
        // great
      }
      else {
        path = "file:/" + path; // NOSONAR : variable reassigned
      }
      return new URL(path);
    }
    catch (MalformedURLException mue) {
      log.error("Impossible to resolve the URL for the path: " + path);
    }
    return null;
  }

  /**
   * change the parent dir and the extension of the given source file.
   *
   * @param source
   * @param destParentDir
   * @param newExtension
   * @param createParentDir
   * @return
   */
  public static final String createFilePath(File source, String destParentDir, String newExtension, boolean createParentDir) {

    StringBuilder path = new StringBuilder(source.getParentFile()
        .getParent()).append(File.separator)
            .append(destParentDir);
    if (createParentDir) {
      File dir = new File(path.toString());
      if (!dir.exists())
        dir.mkdirs();
    }
    int extIdx = source.getName()
        .lastIndexOf('.');
    StringBuilder r = path.append(File.separator);
    if (extIdx < source.getName()
        .length() - 5) {
      r.append(source.getName())
          .append(newExtension);
    }
    else {
      r.append(source.getName()
          .substring(0, extIdx))
          .append(newExtension);
    }
    return r.toString();

  }

  /**
   * if absolutePath = ${xtrade.work.dir}\20040707.2112\sagem-orders-xml\xedi\order_9205126598 .xedi the data uri should be like this :
   * /20040707.2112/sagem-orders-xml/xedi/order_9205126598.xedi
   *
   * @param basePath
   * @param absolutePath
   * @return
   */
  public static final String getUri(String basePath, String absolutePath) {
    String dataUri = "/";

    String uri = absolutePath.replace('\\', '/')
        .substring(basePath.length());
    if (uri.startsWith("/"))
      uri = uri.substring(1);
    if (dataUri.endsWith("/"))
      uri = dataUri + uri;
    else
      uri = dataUri + "/" + uri;
    return uri;
  }

  /**
   * @param filePath
   * @param e
   */
  public static void createLogFile(String filePath, Exception e) {
    write(e.toString(), filePath);
  }

  /*
   * SERIALIZATION
   */

  /**
   * @param file
   * @return
   * @throws IOException
   */
  public static byte[] toByteArray(File file) throws IOException {
    try (
        InputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      copy(in, out);

      return out.toByteArray();
    }
  }

  /**
   * @param bytes
   * @param dest
   * @return File
   * @throws IOException
   */
  public static File toFile(byte[] bytes, File dest) throws IOException {
    try (
        InputStream in = new ByteArrayInputStream(bytes);
        OutputStream out = new FileOutputStream(dest)) {
      copy(in, out);
      return dest;
    }
  }

  /**
   * file encoding
   *
   * @param file
   * @return
   */
  public static String getFileEncoding(File file) {
    try (InputStreamReader reader = new FileReader(file, defaultCharset())) {
      return reader.getEncoding();
    }
    catch (Exception e) {
      log.debug("Error getting getFileEncoding() : {}", getRootCauseMessage(e));
      log.error("Impossible to get the file encoding for: " + file);
    }
    return null;
  }

  /**
   * @param stream
   * @param filePath
   */
  public static boolean write(String stream, String filePath) {
    if (filePath != null) {
      File file = new File(filePath);
      return write(stream, file);
    }
    return false;
  }

  /**
   * @param stream
   * @param file
   */
  public static boolean write(String stream, File file) {
    try (Writer out = new BufferedWriter(new FileWriter(file, defaultCharset()))) {
      file.getParentFile()
          .mkdirs();
      out.write(stream);
    }
    catch (IOException ioe) {
      log.error("Can not create file " + file.getPath(), ioe);
      return false;
    }
    return true;
  }

  /*
   * FILENAME & EXTENSION
   */

  public static String getFileExt(File file) {
    if (file == null)
      return "";
    String name = file.getName();
    int extIdx = name.lastIndexOf('.');
    return extIdx != -1 ? name.substring(extIdx) : "";
  }

  public static String getFileNameWithoutExt(File file) {
    if (file == null)
      return "";
    String name = file.getName();
    int extIdx = name.lastIndexOf('.');
    return extIdx != -1 ? name.substring(0, extIdx) : name;
  }

  public static String getFileNameWithoutExt(String path) {
    if (path == null)
      return "";

    String uxpath = path.replace('\\', '/');
    if (uxpath.indexOf('/') != -1)
      return getFileNameWithoutExt(new File(uxpath));

    int extIdx = path.lastIndexOf('.');
    return extIdx == -1 ? path : path.substring(0, extIdx);
  }

  /**
   * Increments the name of the given file if the same file exists. Example: . [/home/user/filename.txt].exists() ->
   * /home/user/filename-1.txt . [/home/user/filename-1.txt].exists() -> /home/user/filename-2.txt . ...
   *
   * @param file
   * @return file
   */
  public static final File incrementFileName(File file) {
    if (file == null)
      return null;

    File parentFile = file.getParentFile();
    String filenameWithoutExt = getFileNameWithoutExt(file);
    String fileExt = getFileExt(file);
    int idx = 1;
    File r = file;
    while (r.exists()) {
      r = new File(parentFile, new StringBuilder(filenameWithoutExt).append('-')
          .append(idx++)
          .append(fileExt)
          .toString());
    }

    return r;
  }

  /**
   * @param resource
   * @return the file of the given resource.
   */
  public static final File getResourceFile(final String resource) {
    if (StringHelper.isBlank(resource))
      return null;

    final URL url = new FileHelper().getClass()
        .getResource(resource);
    return url == null ? null : new File(url.getFile());
  }

  /*
   * FILE READING
   */

  /**
   * @param file
   */
  public static String read(File file) {
    try {
      return readFileToString(file, defaultCharset());
    }
    catch (IOException e) {
      log.error("Error reading file: " + file, e);
      return null;
    }
  }

  /**
   * @since COM-64
   * @see FileHelper#read(File, ChainableReader...)
   * @param f file to read.
   * @param lines of lines to be read. Defaults to "10" A negative value means that all lines are passed (useful with skip)
   * @param skip of lines to be skipped (from the beginning). Defaults to "0"
   * @return the last first lines from the file supplied to it.
   */
  public static final String readHead(final File f, long lines, long skip) {
    HeadFilter hf = new HeadFilter();
    hf.setLines(lines);
    hf.setSkip(skip);
    return read(f, hf);
  }

  /**
   * @since COM-64
   * @see FileHelper#read(File, ChainableReader...)
   * @param f file to read.
   * @param lines Number of lines to be read. Defaults to "10". A negative value means that all lines are passed (useful with skip)
   * @param skip Number of lines to be skipped (from the end). Defaults to "0"
   * @return the last few lines from the file supplied to it.
   */
  public static final String readTail(final File f, long lines, long skip) {
    TailFilter tf = new TailFilter();
    tf.setLines(lines);
    tf.setSkip(skip);
    return read(f, tf);
  }

  /**
   * @since COM-64
   * @param f file to read.
   * @param filters filter (see <a href= "http://ant.apache.org/manual/CoreTypes/filterchain.html"
   *          >http://ant.apache.org/manual/CoreTypes/filterchain.html</a>)
   * @return the filtered read string from the given file.
   */
  public synchronized static final String read(final File f, final ChainableReader... filters) {
    if (f == null || !f.isFile() || !f.canRead() || filters == null)
      return null;

    // creates task
    LoadFile loadFile = new LoadFile();
    Project project = createProject();
    loadFile.setProject(project);
    loadFile.setProperty(READ_RESULT_PROPERTY);
    loadFile.setQuiet(false);
    loadFile.setSrcFile(f);
    loadFile.setTaskName("READ");

    // adds filters
    FilterChain fc = new FilterChain();
    for (ChainableReader filter : filters) {
      if (filter == null)
        continue;
      fc.add(filter);
    }
    loadFile.addFilterChain(fc);

    // executes
    loadFile.execute();

    return (String) project.getProperties()
        .remove(READ_RESULT_PROPERTY);
  }

  public static final long getLineCount(File f) {
    if (f == null || !f.isFile() || !f.canRead())
      return 0l;

    try {
      return lines(f.toPath()).count();
    }
    catch (SecurityException | IOException e) {
      log.error("Error counting line for file: " + f, e);
    }
    return 0l;
  }

  /*
   * CONTENT TYPE
   */

  /**
   * @param file to check
   * @return the content type of the given file
   * @since 2.5 COM-93
   */
  public static final String getContentType(final File file) {
    return file == null
        ? null
        : getContentType(file.toPath());
  }

  /**
   * @param path to check
   * @return the content type of the given path
   * @since 2.5 COM-93
   */
  public static final String getContentType(final Path path) {
    String mime = null;
    if (path == null || !Files.isRegularFile(path))
      return mime;

    // Tika detection
    final Metadata metadata = new Metadata();
    try (final TikaInputStream input = get(path, metadata)) {
      mime = getTika()
          .getDetector()
          .detect(input, metadata)
          .getBaseType()
          .toString();
      if (mime != null)
        return mime;
    }
    catch (Exception ignored) {
      log.debug("Content type not found using Tika: {} ({})",
          path, getRootCauseMessage(ignored));
    }

    // URL detection
    URLConnection connection = null;
    try {
      connection = path.toUri()
          .toURL()
          .openConnection();
      mime = connection.getContentType();
      if (isNotBlank(mime) &&
          !UNKNOWN.getDefaultMime()
              .equals(mime)) {
        return mime;
      }
    }
    catch (Exception ignored) {
      log.debug("Content type not found using URL connection: {} ({})",
          path, getRootCauseMessage(ignored));
    }
    finally {
      if (connection != null) {
        try {
          close(connection);
          closeQuietly(connection.getInputStream());
        }
        catch (IOException ignored) {
          log.trace("Error closing URL connection on: " + path, ignored);
        }
      }
    }

    // NIO detection
    try {
      mime = probeContentType(path);
      if (isNotBlank(mime))
        return mime;
    }
    catch (Exception ignored) {
      log.debug("Content type not found using NIO: {} ({})",
          path, getRootCauseMessage(ignored));
    }

    // Activation detection
    try {
      return MimetypesFileTypeMap
          .getDefaultFileTypeMap()
          .getContentType(path.toFile());
    }
    catch (Exception ignored) {
      log.debug("Content type not found using Activation: {} ({})",
          path, getRootCauseMessage(ignored));
    }

    return null;
  }

  private static TikaConfig getTika() throws TikaException, IOException {
    if (tika == null)
      tika = new TikaConfig();
    return tika;
  }

  /**
   * Get the most recent modification time of files in the given path. If path is a file, the result is trivially that file modification
   * time. If path is a directory, it is recursively visited and the most recent modification time of it's file descendants is returned.
   * Only regular files are taken into account.
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static final FileTime getLatestModifiedTime(Path path) throws IOException {
    LatestModifiedTimeFileVisitor visitor = new LatestModifiedTimeFileVisitor();
    walkFileTree(path, visitor);
    return visitor.getYoungest();
  }

  /**
   * @param file - {@link File}
   * @param numberOfBytes
   * @return a {@link String} containing the first {@code numberOfBytes} from a file
   */
  public static String readBytesFromFile(File file, int numberOfBytes) {
    if (file == null) {
      return EMPTY;
    }
    byte[] buffer = new byte[numberOfBytes];
    try (FileInputStream fi = new FileInputStream(file)) {
      fi.read(buffer);
      // I'm not sure about the encoding
      return new String(buffer, defaultCharset());
    }
    catch (IOException e) {
      log.error("Could not read from file {} {}", file, e.getMessage());
      return EMPTY;
    }
  }

  /**
   * @param basePath -
   * @param subdirectories can contains directories, * that represents all subdirectories, and ** that represents all subdirectories of all
   *          subdirectories of curent folder
   * @return
   * @throws IOException
   */
  public static List<File> getDirectoriesByPattern(String basePath, String subdirectories) throws IOException {
    Predicate<? super Path> predicate = Files::isDirectory;
    List<File> currentFolders = new ArrayList<>();
    currentFolders.add(new File(basePath));
    if (StringUtils.isNotEmpty(subdirectories)) {
      String[] directories = subdirectories.split("/");
      for (int i = 0; i < directories.length; i++) {
        String director = directories[i];
        if ("*".equals(director)) {
          currentFolders = getAllFilesByType(currentFolders, 0, predicate);
          if (currentFolders.isEmpty()) {
            log.error("The folder " + director + " has no subdirectories");
            return emptyList();
          }
        }
        else if ("**".equals(director)) {
          String[] lastDirectories;
          if (i == directories.length - 1) {
            lastDirectories = new String[0];
          }
          else {
            lastDirectories = Arrays.copyOfRange(directories, i + 1, directories.length);
          }
          return getAllSubfoldersFilteredByPattern(currentFolders, lastDirectories);

        }
        else {
          currentFolders = Optional.ofNullable(currentFolders)
              .orElseGet(() -> new ArrayList<>())
              .stream()
              .flatMap(file -> {
                try {
                  return Files.walk(file
                      .toPath(), 1)
                      .filter(p -> Files.isDirectory(p))
                      .filter(p -> p.toFile()
                          .compareTo(file) != 0)
                      .filter(p -> director.equals(p.getFileName()
                          .toString()))
                      .map(p -> p.toFile());
                }
                catch (IOException e) {
                  return null;
                }
              })
              .collect(Collectors.toList());
          if (currentFolders.isEmpty()) {
            log.info("The folder " + director + " does not exist in current path");
            return emptyList();
          }
        }
      }
      return currentFolders;
    }

    return getAllFilesByType(currentFolders, 0, predicate);

  }

  /**
   * @param files - folders where has to be searched files
   * @return the files found
   */
  public static List<File> getAllFilesByType(List<File> files, int iterations, Predicate<? super Path> predicate) {
    if (iterations < 0 || iterations > 10) {
      throw new InvalidParameterException("Value of iterations argument must be between 0 and 10");
    }
    List<File> directories = Optional.ofNullable(files)
        .orElseGet(() -> new ArrayList<>())
        .stream()
        .flatMap(file -> {
          try {
            return Files.list(Paths.get(file.getAbsolutePath()))
                .filter(predicate);
          }
          catch (IOException e) {
            return null;
          }
        })
        .map(path -> path.toFile())
        .collect(Collectors.toList());

    if (iterations == 0)
      return directories;
    else return getAllFilesByType(directories, iterations - 1, predicate);

  }

  public static List<File> getAllSubfoldersFilteredByPattern(List<File> startPath, String[] endPath) {
    int noOfDirectories = startPath.get(0)
        .toPath()
        .getNameCount();
    List<File> resultFolders = Optional.ofNullable(startPath)
        .orElseGet(() -> new ArrayList<>())
        .stream()
        .flatMap(file -> {
          try {
            return Files.walk(file
                .toPath(), 20)
                .filter(p -> Files.isDirectory(p))
                .filter(p -> p.compareTo(file.toPath()) != 0)
                .filter(p -> p.getNameCount() > noOfDirectories + endPath.length)
                .filter(p -> {
                  Path currentFolder = p;
                  for (int i = endPath.length - 1; i >= 0; i--) {
                    String nameOfFolder = currentFolder.getFileName()
                        .toString();
                    if (!nameOfFolder
                        .equals(endPath[i]) && !("*").equals(endPath[i]))
                      return false;
                    else {
                      currentFolder = currentFolder.getParent();
                    }
                  }
                  return true;
                })
                .map(p -> p.toFile());
          }
          catch (IOException e) {
            return null;
          }
        })
        .collect(Collectors.toList());

    return resultFolders;
  }

  /**
   * Returns a {@link List} containing all {@code subdirectories} of {@code directory}</br>
   * <b>Note:It is recursive</b>
   *
   * @param directory - {@link File}
   * @return {@link List} of {@link File files}
   */
  public static List<File> findAllSubdirs(File directory) {
    if (directory == null || !directory.isDirectory()) {
      return emptyList();
    }
    List<File> subdirs = Arrays.asList(directory.listFiles(File::isDirectory));
    subdirs = new ArrayList<File>(subdirs);
    List<File> deepSubdirs = new ArrayList<File>();
    for (File subdir : subdirs) {
      deepSubdirs.addAll(findAllSubdirs(subdir));
    }
    subdirs.addAll(deepSubdirs);
    return subdirs;
  }
}

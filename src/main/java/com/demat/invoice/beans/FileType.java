package com.demat.invoice.beans;

import com.demat.invoice.utils.StringHelper;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.demat.invoice.utils.FileHelper.getContentType;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * FileType enum. TODO: add ASCII and/or HEX Identifying Characters use (http://filext.com/ )
 */
public enum FileType {
  UNKNOWN("",
      true,
      "content/unknown"), // 0
  DIR("",
      true,
      "directory/dir"), // 1
  // -- xML --
  XML(".xml",
      false, // 2
      "text/xml",
      "application/xml",
      "application/x-xml"),
  XSL(".xsl",
      true, // 3
      "text/xsl",
      "text/xml",
      "application/xml",
      "application/x-xml"),
  XSD(".xsd",
      false, // 4
      "text/xsd",
      "text/xml",
      "application/xml",
      "application/x-xml"),
  HTML(".html",
      false, // 5
      "text/html"),
  // -- TEXT --
  TXT(".txt",
      false, // 6
      "text/plain",
      "application/txt",
      "browser/internal",
      "text/anytext",
      "widetext/plain",
      "widetext/paragraph"),
  // -- PDF --
  PDF(".pdf",
      true, // 7
      "application/pdf",
      "application/x-pdf",
      "application/acrobat",
      "applications/vnd.pdf",
      "text/pdf",
      "text/x-pdf"),
  // -- OFFICE --
  CSV(".csv",
      false, // 8
      "text/comma-separated-values",
      "text/csv",
      "application/csv",
      "application/excel",
      "application/vnd.ms-excel",
      "application/vnd.msexcel"),
  EXCEL(".xls",
      true, // 9
      "application/msexcel",
      "application/x-msexcel",
      "application/x-ms-excel",
      "application/vnd.ms-excel",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // for .xlsx extension
      "application/x-excel",
      "application/x-dos_ms_excel",
      "application/xls",
      "application/x-xls",
      "zz-application/zz-winassoc-xls"),
  RTF(".rtf",
      true, // 10
      "application/rtf",
      "application/x-rtf",
      "text/rtf",
      "text/richtext",
      "application/msword",
      "application/doc",
      "application/x-soffice"),
  WORD(".doc",
      true, // 11
      "application/msword",
      "application/doc",
      "appl/text",
      "application/vnd.msword",
      "application/vnd.ms-word",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // for .docx extension
      "application/winword",
      "application/word",
      "application/x-msw6",
      "application/x-msword",
      "zz-application/zz-winassoc-doc"),
  // -- ZIP --
  ARCHIVE(".zip",
      true, // 12
      "application/zip",
      "application/x-zip",
      "application/x-zip-compressed",
      "application/octet-stream",
      "application/x-compress",
      "application/x-compressed",
      "multipart/x-zip"),
  // -- EDI --
  XEDI(".xedi",
      false,
      XML.mimes), // 13
  EDI(".edi",
      false,
      TXT.mimes), // 14
  EDIUNIT(".uedi",
      false,
      TXT.mimes), // 15
  // -- SAP --
  SAP(".idoc",
      false,
      TXT.mimes), // 16
  // -- REPORT --
  REPORT(".xml",
      false,
      XML.mimes), // 17
  // -- JBPM --
  PROCESS_ARCHIVE(".par",
      true,
      ARCHIVE.mimes), // 18
  PROCESS_DEFINITION(XML.extension,
      false,
      XML.mimes), // 19
  // -- JELLY --
  JSL(".jsl",
      true,
      XML.mimes), // 20
  // -- BIRT --
  RPTDESIGN(".rptdesign",
      false,
      XML.mimes), // 21
  JSF(".jsf",
      false,
      "application/xhtml+xml"), // 22
  // -- EBICS --
  EBICS(".dat",
      false,
      TXT.mimes), // 23
  EBICS_HEADER(".txt",
      false,
      TXT.mimes), // 24
  // -- EXE --
  EXE(".exe",
      true,
      "application/octet-stream",
      "application/exe",
      "application/x-exe",
      "application/dos-exe",
      "application/x-winexe",
      "application/msdos-windows",
      "application/x-msdos-program"), // 25
  // -- JavaScript --
  JS(".js",
      false, // 26
      "text/javascript",
      "application/x-javascript"),
  // -- JSON --
  JSON(".json",
      false, // 27
      "application/json",
      "application/x-javascript",
      "text/javascript",
      "text/x-javascript",
      "text/x-json"),
  BSON(".bson",
      true, // 28
      "application/bson"),
  // -- PostScript --
  POSTSCRIPT(".ps",
      false, // 29
      "application/postscript",
      "application/ps",
      "application/x-postscript",
      "application/x-ps",
      "text/postscript",
      "application/x-postscript-not-eps"),
  // -- XCBL --
  XCBL(XML.extension,
      false, // 30
      XML.mimes),
  // -- Error --
  ERROR(".err",
      true, // 31
      JSON.mimes),
  // -- Properties --
  PROPERTIES(".properties",
      false, // 32
      TXT.mimes),
  // -- RTE --
  RTE(".rte",
      false, // 33
      TXT.mimes),
  // -- Info --
  INFO(".nfo",
      false, // 34
      TXT.mimes),
  // -- IMG --
  PNG(".png",
      true, // 35
      "image/png"),
  GIF(".gif",
      true, // 36
      "image/gif"),
  JPEG(".jpeg",
      true, // 37
      "image/jpeg"),
  JPG(".jpg",
      true, // 38
      "image/jpeg"),
  BMP(".bmp",
      true, // 39
      "image/bmp"),
  TIF(".tif",
      true, // 40
      "image/tif"),
  TIFF(".tiff",
      true, // 41
      "image/tiff"),
  POWERPOINT(".ppt",
      true, // 42
      "application/vnd.ms-powerpoint",
      "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      "application/vnd.openxmlformats-officedocument.presentationml.template",
      "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
      "application/vnd.ms-powerpoint.addin.macroEnabled.12",
      "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
      "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
      "application/vnd.ms-powerpoint.slideshow.macroEnabled.12"),

  // -- PDF/A-3 -- 43
  PDFA3(PDF.extension,
      true,
      PDF.mimes),

  // -- CER -- 44
  CER(".cer",
      false,
      "application/pkix-cert"),
  // --SCH --45
  SCH(".sch",
      true,
      "text/sch",
      "text/xsl",
      "text/xml",
      "application/xml",
      "application/x-xml");

  private String[] mimes;
  private String extension;
  private final boolean binary;

  FileType(String ext, boolean binary, String... mimes) {
    this.extension = ext;
    this.binary = binary;
    this.mimes = mimes;
  }

  public String getExtension() {
    return extension;
  }

  public String[] getMimes() {
    return mimes;
  }

  public String getDefaultMime() {
    return mimes[0];
  }

  public boolean isXML() {
    return StringHelper.join(mimes)
        .contains("xml");
  }

  public boolean isBinary() {
    return binary;
  }

  public boolean isImage() {
    return StringHelper.join(mimes)
        .contains("image");
  }

  public static boolean isType(File file, FileType... types) {
    if (file == null || isEmpty(types))
      return false;

    String filename = file.getName();
    for (FileType type : types) {
      if (type != null && filename.endsWith(type.extension))
        return true;
    }

    return false;
  }

  public static FileType getType(File file) {
    return getType(file, true);
  }

  public static FileType getType(File file, boolean useContentType) { // NOSONAR : cyclomatic complexity > 10
    if (file == null)
      return null;

    // Based on the extension
    String ext = FilenameUtils.getExtension(file.getAbsolutePath());
    if (isNotBlank(ext)) {
      ext = ext.toLowerCase();
      String dotext = "." + ext;
      List<FileType> potentialTypes = new ArrayList<>();
      for (FileType type : values()) {
        if (isNotBlank(type.getExtension())) {
          // equality -> returns
          if (type.getExtension()
              .equalsIgnoreCase(dotext)) // NOSONAR : function too deeply
            return type;
          // potential match (ex: xdoc -> doc)
          if (ext.length() > 3 && ext.contains(type.getExtension())) // NOSONAR : function too deeply
            potentialTypes.add(type);
        }
      }
      // returns the potential type found
      // if single to avoid ambiguity
      // resolution
      if (potentialTypes.size() == 1)
        return potentialTypes.get(0);
    }

    // Based on content type
    if (useContentType) {
      final String contenType = getContentType(file);
      if (isNotBlank(contenType)) {
        return stream(values())
            .filter(type -> isNotEmpty(type.getMimes()) &&
                stream(type.getMimes())
                    .anyMatch(mime -> isNotBlank(mime) && mime.contains(contenType)))
            .findFirst()
            .orElse(UNKNOWN);
      }
    }

    return UNKNOWN;
  }

  public static FileType valueOf(MediaType mediaType) {
    if (mediaType != null) {
      boolean concrete = mediaType.isConcrete();
      for (FileType ft : values()) {
        if (stream(ft.mimes)
            .filter(mime -> (concrete && mime.equals(mediaType.toString())) ||
                (!concrete && mediaType.isCompatibleWith(MediaType.valueOf(mime))))
            .findFirst()
            .isPresent()) {
          return ft;
        }
      }
    }

    return UNKNOWN;
  }

}

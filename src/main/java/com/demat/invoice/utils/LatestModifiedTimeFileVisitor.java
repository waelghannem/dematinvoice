package com.demat.invoice.utils;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.attribute.FileTime.fromMillis;

class LatestModifiedTimeFileVisitor extends SimpleFileVisitor<Path> {
  private FileTime youngest = fromMillis(0);

  public FileTime getYoungest() {
    return youngest;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
    if (attrs.isRegularFile()) {
      FileTime lastModifiedTime = attrs.lastModifiedTime();
      youngest = youngest.compareTo(lastModifiedTime) > 0 ? youngest : lastModifiedTime;
    }
    return CONTINUE;
  }
}

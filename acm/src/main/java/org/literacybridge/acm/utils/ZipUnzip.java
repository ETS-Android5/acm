package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUnzip {
  File baseInDir;

  private ZipUnzip(File baseDirectory) {
    baseInDir = baseDirectory;
  }

  private void addDirectory(ZipOutputStream zout, File fileSource,
      boolean includeBaseDir, boolean includeChildren) throws IOException {
    if (includeBaseDir || fileSource != baseInDir) {
      String relativeDirName = baseInDir.toURI().relativize(fileSource.toURI())
          .getPath();
      zout.putNextEntry(new ZipEntry(relativeDirName));
    }
    if (!includeChildren)
      return;
    File[] files = fileSource.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (files[i].isDirectory()) {
        addDirectory(zout, files[i], false, true);
        continue;
      }
      try {
        String relativeFileName = baseInDir.toURI().relativize(files[i].toURI())
            .getPath();
        byte[] buffer = new byte[1024];
        FileInputStream fin = new FileInputStream(files[i]);
        zout.putNextEntry(new ZipEntry(relativeFileName));
        int length;
        while ((length = fin.read(buffer)) > 0) {
          zout.write(buffer, 0, length);
        }
        zout.closeEntry();
        fin.close();
      } catch (IOException ioe) {
        System.out.println("IOException :" + ioe);
      }
    }
  }

  public static void zip(File inDir, File outFile) throws IOException {
    zip(inDir, outFile, false);
  }

  public static void zip(File inDir, File outFile, boolean includeBaseDir)
      throws IOException {
    ZipUnzip zipper;
    if (includeBaseDir) {
      zipper = new ZipUnzip(inDir.getParentFile());
    } else {
      zipper = new ZipUnzip(inDir);
    }
    outFile.delete();
    outFile.getParentFile().mkdirs();
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outFile));
    zipper.addDirectory(zout, inDir, includeBaseDir, true);
    zout.close();
    // System.out.println("Zip file has been created!");
  }

  public static void zip(File inDir, File outFile, boolean includeBaseDir,
      String[] subdirs) throws IOException {
    ZipUnzip zipper;
    if (includeBaseDir) {
      zipper = new ZipUnzip(inDir.getParentFile());
    } else {
      zipper = new ZipUnzip(inDir);
    }
    outFile.delete();
    outFile.getParentFile().mkdirs();
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outFile));
    zipper.addDirectory(zout, inDir, includeBaseDir, false);
    for (String dir : subdirs) {
      File f = new File(inDir, dir);
      if (f.exists()) {
        zipper.addDirectory(zout, f, false, true);
      }
    }
    zout.close();
    // System.out.println("Zip file has been created!");
  }

  public static void unzip(File inFile, File outDir) throws IOException {
    File parentDir;

    parentDir = outDir;
    ZipFile zfile = new ZipFile(inFile);
    Enumeration<? extends ZipEntry> entries = zfile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      File file = new File(parentDir, entry.getName());
      if (entry.isDirectory()) {
        file.mkdirs();
      } else {
        file.getParentFile().mkdirs();
        InputStream in = zfile.getInputStream(entry);
        try {
          OutputStream out = new FileOutputStream(file);
          byte[] buffer = new byte[1024];
          while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
              break;
            }
            out.write(buffer, 0, readCount);
          }
        } finally {
          in.close();
        }
      }
    }
  }

}

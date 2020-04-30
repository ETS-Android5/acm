package org.literacybridge.acm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.powermock.api.easymock.PowerMock.mockStatic;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for simple DropboxFinder.
 */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({ DropboxFinderTest.class, DropboxFinder.class })
public class DropboxFinderTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private File mockDropbox(boolean business) throws IOException {
    File dropbox = tmp.newFolder(business?"Dropbox (Amplio)":"Dropbox");
    File LB_software = new File(dropbox, "LB-software");
    File ACM_Install = new File(LB_software, "ACM-Install");
    File ACM = new File(ACM_Install, "ACM");
    File software = new File (ACM, "software");
    software.mkdirs();
    File acm_jar = new File(software, "acm.jar");
    acm_jar.createNewFile();
    return dropbox;
  }

//  @Test
//  public void testMissingEnvironment() {
//    mockStatic(System.class);
//    // Pretend we're on Windows
//    expect(System.getProperty("os.name")).andReturn("Windows");
//    expect(System.getenv("APPDATA")).andReturn(null);
//    expect(System.getenv("LOCALAPPDATA")).andReturn(null);
//
//    DropboxFinder dbFinder = mock(DropboxFinder.class);
//    expect(dbFinder.onWindows()).andReturn(true);
//
//    boolean gotException = false;
//    File path = null;
//    try {
//      path = dbFinder.getInfoFile();
//    } catch (RuntimeException e) {
//      gotException = true;
//    }
//    assertTrue(gotException);
//  }
//
//  @Test
//  public void testGetInfoFilePathForNix() {
//    mockStatic(System.class);
//    // Pretend we're on Mac
//    expect(System.getProperty("os.name")).andReturn("Mac OSX");
//    expect(System.getProperty("user.home")).andReturn("/home/LB");
//    DropboxFinder dbFinder = mock(DropboxFinder.class);
//    expect(dbFinder.onWindows()).andReturn(false);
//
//    // If, by mistake, we think we're on Windows, we'll use LOCALAPPDATA to find
//    // the path:
//    expect(System.getenv("APPDATA"))
//        .andReturn("r:\\Users\\LB\\AppData\\Roaming");
//    expect(System.getenv("LOCALAPPDATA"))
//        .andReturn("r:\\Users\\LB\\AppData\\Local");
//
//    File infoFile = dbFinder.getInfoFile();
//    String expected = File.separator + "home" + File.separator + "LB"
//        + File.separator + ".dropbox" + File.separator + "info.json";
//    assertEquals(expected, infoFile.getPath());
//  }
//
//  @Test
//  public void testGetInfoFilePathForWindows() {
//    DropboxFinder dbFinder = mock(DropboxFinder.class);
//    mockStatic(System.class);
//    // Pretend we're on Windows
//    expect(System.getProperty("os.name")).andReturn("Windows");
//    expect(System.getenv("APPDATA"))
//        .andReturn("r:\\Users\\LB\\AppData\\Roaming");
//    expect(System.getenv("LOCALAPPDATA"))
//        .andReturn("r:\\Users\\LB\\AppData\\Local");
//    expect(dbFinder.onWindows()).andReturn(true);
//
//    // If, by mistake, we think we're on OS/X or Linux, we'll use this to find
//    // the path:
//    expect(System.getProperty("user.home")).andReturn("/home/LB");
//
//    File infoFile = dbFinder.getInfoFile();
//    String expected = "r:\\Users\\LB\\AppData\\Local" + File.separator
//        + "Dropbox" + File.separator + "info.json";
//    System.out.printf("Expecting %s\ngot%s\n", expected, infoFile.getPath());
//    assertEquals(expected, infoFile.getPath());
//  }
//
//  @Test
//  public void testParseDrobpoxPersonal() throws IOException {
//    File dbxDirPer = mockDropbox(true);
//    String dbxPathPer = dbxDirPer.getAbsolutePath();
//    dbxPathPer = dbxPathPer.replace('\\', '/');
//    DropboxFinder dbFinder = mock(DropboxFinder.class);
//    // String jsonString = "{\"business\": {\"path\": \"/Users/bill/Dropbox
//    // (Literacy Bridge)\", \"host\": 4929547026}}";
//    String jsonString = "{\"personal\": {\"path\": \""+dbxPathPer+"\", \"host\": 4929547026}}";
//    InputStream jsonStream = null;
//    try {
//      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
//    } catch (UnsupportedEncodingException e) {
//      assertTrue(false); // fail the test.
//    }
//
//    String path;
//    path = dbFinder.getDropboxPathFromInputStream(jsonStream);
//
//    assertEquals(dbxPathPer, path);
//  }
//
//  @Test
//  public void testParseDrobpoxBusiness() throws IOException {
//    File dbxDirBus = mockDropbox(true);
//    String dbxPathBus = dbxDirBus.getAbsolutePath();
//    dbxPathBus = dbxPathBus.replace('\\', '/');
//    DropboxFinder dbFinder = mock(DropboxFinder.class);
//    String jsonString = "{\"business\": {\"path\": \""+dbxPathBus+"\", \"host\": 4929547026}}";
//    // String jsonString = "{\"personal\": {\"path\": \"/Users/LB/Dropbox\",
//    // \"host\": 4929547026}}";
//    InputStream jsonStream = null;
//    try {
//      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
//    } catch (UnsupportedEncodingException e) {
//      assertTrue(false); // fail the test.
//    }
//
//    String path;
//    path = dbFinder.getDropboxPathFromInputStream(jsonStream);
//
//    assertEquals(dbxPathBus, path);
//  }
//
//  @Test
//  public void testParseDrobpoxBoth() throws IOException {
//    File dbxDirPer = mockDropbox(false);
//    String dbxPathPer = dbxDirPer.getAbsolutePath();
//    dbxPathPer = dbxPathPer.replace('\\', '/');
//    File dbxDirBus = mockDropbox(true);
//    String dbxPathBus = dbxDirBus.getAbsolutePath();
//    dbxPathBus = dbxPathBus.replace('\\', '/');
//    DropboxFinder dbFinder = mock(DropboxFinder.class);
//    String jsonString = "{\"business\": {\"path\": \""+dbxPathBus+"\", \"host\": 4929547026},"
//        + "\"personal\": {\"path\": \"/Users/LB/Dropbox\", \"host\": 4929547026}}";
//    InputStream jsonStream = null;
//    try {
//      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
//    } catch (UnsupportedEncodingException e) {
//      assertTrue(false); // fail the test.
//    }
//
//    String path;
//    path = dbFinder.getDropboxPathFromInputStream(jsonStream);
//
//    assertEquals(dbxPathBus, path);
//  }
//
//  @Test
//  public void testParseBadJson() {
//    DropboxFinder dbFinder = mock(DropboxFinder.class);
//    String jsonString = "This is not a JSON string";
//    InputStream jsonStream = null;
//    try {
//      jsonStream = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
//    } catch (UnsupportedEncodingException e) {
//      assertTrue(false); // fail the test.
//    }
//
//    String path;
//    path = dbFinder.getDropboxPathFromInputStream(jsonStream);
//
//    assertEquals("", path);
//  }

}

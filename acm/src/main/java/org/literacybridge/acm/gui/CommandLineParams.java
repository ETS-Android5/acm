package org.literacybridge.acm.gui;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CommandLineParams {
  @Option(name = "--sandbox", aliases = {"-s"}, usage = "to enter sandbox mode")
  public boolean sandbox;

  @Option(name = "--title", aliases = {"-title"}, usage = "to set name of ACM to be displayed in title bar")
  public String titleACM;

  @Option(name = "--headless", aliases = {"-no_ui", "--no_ui"}, usage = "start the system without showing the UI")
  public boolean disableUI;

  @Option(name = "--allcategories", usage = "show all categories, regardless of whitelisting")
  public boolean allCategories;

  @Argument
  public String sharedACM;
}

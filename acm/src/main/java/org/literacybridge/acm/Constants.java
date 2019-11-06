package org.literacybridge.acm;

import java.io.File;
import java.util.regex.Pattern;

public class Constants {
  public final static String ACM_VERSION = "r1911050"; // yy mm dd n
  public final static String LiteracybridgeHomeDirName = "LiteracyBridge";
  public final static String ACM_DIR_NAME = "ACM";
  public final static String CACHE_DIR_NAME = "cache";
  public final static String TempDir = "temp";
  public final static String DBHomeDir = "db";
  public final static String RepositoryHomeDir = "content";
  public final static String LuceneIndexDir = "index";
  public final static String TBLoadersHomeDir = "TB-Loaders";
  public final static String ProgramSpecDir = "programspec";
  public final static String DefaultSharedDB = "ACM-test/" + DBHomeDir;
  public final static String DefaultSharedRepository = "ACM-test/"
      + RepositoryHomeDir;
  public final static String TBDefinitionsHomeDirName = "TB-definitions";
  public final static String USERS_APPLICATION_PROPERTIES = "acm_config.properties";
  public final static String CONFIG_PROPERTIES = "config.properties";
  public final static String CHECKOUT_PROPERTIES_SUFFIX = "-checkedOut.properties";
  public final static String USER_WRITE_LOCK_FILENAME = "locked.txt";
  public final static String DB_ACCESS_FILENAME = "accessList.txt";
  public final static String USER_FEEDBACK_WHITELIST_FILENAME = "userfeedback.whitelist";

  public final static File USER_HOME_DIR = new File(
      System.getProperty("user.home", "."));
  public final static long DEFAULT_CACHE_SIZE_IN_BYTES = 2L * 1024L * 1024L * 1024L; // 2GB

  public final static String USER_NAME = "USER_NAME";
  public final static String USER_CONTACT_INFO = "USER_CONTACT_INFO";
  // public final static String DEFAULT_REPOSITORY = "DEFAULT_REPOSITORY";
  // public final static String DEFAULT_DB = "DEFAULT_DB";
  public final static String GLOBAL_SHARE_PATH = "GLOBAL_SHARE_PATH";
  public final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
  public final static String DEVICE_ID_PROP = "DEVICE_ID";
  public final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";
  public final static String PRE_CACHE_WAV = "PRE_CACHE_WAV";
  public final static String CACHE_SIZE_PROP_NAME = "CACHE_SIZE_IN_BYTES";
  public final static String STRICT_DEPLOYMENT_NAMING = "STRICT_DEPLOYMENT_NAMING";
  public final static String USER_FEEDBACK_HIDDEN = "USER_FEEDBACK_HIDDEN";
  public final static String CONFIGURATION_DIALOG = "CONFIGURATION_DIALOG";
  public final static String USE_AWS_LOCKING = "USE_AWS_LOCKING";
  public final static String DEPLOYMENT_CHOICE = "DEPLOYMENT_CHOICE";
  public final static String BATCH_RECORD = "record.log";
  public final static String S3_BUCKET = "acm-logging";
  public final static String FUZZY_THRESHOLD = "FUZZY_THRESHOLD";
  public final static String NOTIFY_LIST = "NOTIFY_LIST";

  // Gather obsolete property names here. We could write code to remove these from the properties file.
  public final static String[] OBSOLETE_PROPERTY_NAMES = {"NEXT_CORRELATION_ID"};

  public final static String CATEGORY_GENERAL_OTHER = "0-0";
  public final static String CATEGORY_TB_SYSTEM = "0-4-1";
  public final static String CATEGORY_TB_CATEGORIES = "0-4-2";
  public final static String CATEGORY_INTRO_MESSAGE = "0-5";
  public final static String CATEGORY_UNCATEGORIZED_FEEDBACK = "9-0";
  public static final String CATEGORY_TOO_SHORT_FEEDBACK = "92-2";
  public static final String CATEGORY_TOO_LONG_FEEDBACK = "92-6";
  public static final String CATEGORY_UNKNOWN_LENGTH_FEEDBACK = "92-8";
  public static final String CATEGORY_TB_INSTRUCTIONS = "0-1";
  public static final String CATEGORY_COMMUNITIES = "0-3";

  public static final int FUZZY_THRESHOLD_MAXIMUM = 100;
  public static final int FUZZY_THRESHOLD_DEFAULT = 80;
  public static final int FUZZY_THRESHOLD_MINIMUM = 60;
}

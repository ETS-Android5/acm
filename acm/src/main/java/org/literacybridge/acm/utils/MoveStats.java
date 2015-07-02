package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.tbloader.TBLoader;

import com.google.common.collect.Sets;

public class MoveStats {

	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			printUsage();
			System.exit(1);
		}		
		File sourceDir = new File(args[0]);
		File targetDir = new File(args[1]);
		if (!(sourceDir.exists() && targetDir.exists())) {
			printUsage();
			System.exit(1);
		}
		
		File[] subdirs = sourceDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return dir.isDirectory() && dir.listFiles().length > 0 && !name.startsWith(".");
			}
		});
		if (subdirs.length > 0) {
			String timeStamp = TBLoader.getDateTime();
			File targetCollection = new File(targetDir,timeStamp);
			targetCollection.mkdir();
			
			for (File subdir:subdirs) {
				//System.out.println("Zipping " + subdir + " and moving to " + targetCollection.getAbsolutePath());
				ZipUnzip.zip(subdir, new File(targetCollection,subdir.getName() + ".zip"), true);
				FileUtils.cleanDirectory(subdir);
			}
			//System.out.println("-----------------------------------------");
			//for (File subdir:subdirs) {
			//	System.out.println("Zipped " + subdir + " and moved to " + targetCollection.getAbsolutePath());
			//}
			System.out.println(targetCollection.getAbsolutePath());
		} else {
			System.out.println("no directories found in target (other than possibly empty or hidden ones)");
		}
	}

	private static void printUsage() {
		System.err.println("java -cp acm.jar:lib/* org.literacybridge.acm.utils.MoveStats source target");
	}


}

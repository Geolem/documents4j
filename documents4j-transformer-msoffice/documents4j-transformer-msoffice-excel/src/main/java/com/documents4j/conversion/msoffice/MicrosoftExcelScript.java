package com.documents4j.conversion.msoffice;

import com.documents4j.throwables.FileSystemInteractionException;
import com.documents4j.util.OsUtils;
import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * VBS Scripts for communicating with MS Excel.
 */
enum MicrosoftExcelScript implements MicrosoftOfficeScript {

    CONVERSION("/excel_convert" + (OsUtils.isWindows()?".vbs":(OsUtils.isMac()?".applescript":".sh"))),
    STARTUP("/excel_start" + (OsUtils.isWindows()?".vbs":(OsUtils.isMac()?".applescript":".sh"))),
    SHUTDOWN("/excel_shutdown" + (OsUtils.isWindows()?".vbs":(OsUtils.isMac()?".applescript":".sh"))),
    ASSERTION("/excel_assert" + (OsUtils.isWindows()?".vbs":(OsUtils.isMac()?".applescript":".sh")));

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftExcelBridge.class);
    private static final Random RANDOM = new Random();

    private final String path;

    private MicrosoftExcelScript(String path) {
        this.path = path;
    }

    public String getName() {
        return path.substring(1);
    }

    public String getRandomizedName() {
        String name = getName();
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex < 0) {
            return String.format("%s%d", name, RANDOM.nextInt());
        } else {
            return String.format("%s%d.%s", name.substring(0, extensionIndex), Math.abs(RANDOM.nextInt()), name.substring(extensionIndex + 1));
        }
    }

    @Override
    public File materializeIn(File folder) {
        File script = new File(folder, getRandomizedName());
        try {
            if (!script.createNewFile()) {
                throw new IOException(String.format("Could not create file %s", script));
            }
            String override = System.getProperty("com.documents4j.conversion.msoffice." + path.substring(1));
            if (override == null) {
                Resources.asByteSource(Resources.getResource(getClass(), path)).copyTo(Files.asByteSink(script));
            } else {
                Files.asByteSource(new File(override)).copyTo(Files.asByteSink(script));
            }
        } catch (IOException e) {
            String message = String.format("Could not copy script resource '%s' to local file system at '%s'", path, folder);
            LOGGER.error(message, e);
            throw new FileSystemInteractionException(message, e);
        }
        return script;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(MicrosoftExcelScript.class)
                .add("resource", path)
                .toString();
    }
}

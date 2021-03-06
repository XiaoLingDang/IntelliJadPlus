/*
 * Copyright 2007 Steve Chaloner
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.stevechaloner.intellijad.decompilers;

import com.google.common.io.Closer;
import com.intellij.openapi.util.io.StreamUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.vfs.JarFile;
import com.intellij.openapi.vfs.JarFile.JarEntry;
import net.stevechaloner.intellijad.console.ConsoleEntryType;

/**
 * JarExtractor will pull a file from a given path and extract it into
 * a directory on the file system.
 *
 * @author Steve Chaloner
 */
class JarExtractor {
    /**
     * Pattern describing class names, including nested classes.
     */
    private static final String CLASS_PATTERN = "((\\$\\w*)?)*";
    private static final String DEFAULT_PACKAGE = "/";

    private String preparePackage(String packageName) {
        return DEFAULT_PACKAGE.equals(packageName) ? "" : packageName;
    }

    private String justFileName(String jarEntryName) {
        int lastIndex = jarEntryName.lastIndexOf("/");
        if (lastIndex > -1) {
            return jarEntryName.substring(lastIndex);
        } else {
            return jarEntryName;
        }
    }
    /**
     * Extract the given file to the target directory specified in the context.
     *
     * @param context     the context of the decompilation operation
     * @param jarFile     the name of the zip file to open
     * @param packageName the package of the class
     * @param className   the name of the class
     * @throws IOException if an error occurs during the operation
     */
    void extract(DecompilationContext context,
                 JarFile jarFile,
                 String packageName,
                 String className) throws IOException {
        Pattern p = Pattern.compile(preparePackage(packageName) + className + CLASS_PATTERN + ".class");

        Enumeration<? extends JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            Matcher matcher = p.matcher(name);
            if (matcher.matches()) {
                context.getConsoleContext().addMessage(ConsoleEntryType.JAR_OPERATION,
                        "message.extracting",
                        entry.getName());
                Closer closer = Closer.create();
                try {
                    InputStream inputStream = closer.register(jarFile.getInputStream(entry));

                    File outputFile = new File(context.getTargetDirectory(), justFileName(entry.getName()));
                    context.getConsoleContext().addMessage(ConsoleEntryType.JAR_OPERATION,
                            "message.extracting-done",
                            entry.getName(), outputFile.getAbsolutePath());
                    outputFile.deleteOnExit();
                    FileOutputStream fos = closer.register(new FileOutputStream(outputFile));
                    StreamUtil.copyStreamContent(inputStream, fos);
                } finally {
                    closer.close();
                }
            }
        }
    }
}

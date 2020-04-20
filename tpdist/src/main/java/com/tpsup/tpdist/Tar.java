package com.tpsup.tpdist;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

public class Tar {
    public static TarArchiveOutputStream createTar(String outputString, String relativeString,
            ArrayList<String> inputStringList, boolean inputIsRelative,
            TarArchiveOutputStream tarArchiveOutputStream) throws IOException {
        relativeString = relativeString.replace("\\", "/").replaceAll("[/]+$", "");
        File outputFile = new File(outputString);
        File relativePath = new File(relativeString);
        if (tarArchiveOutputStream == null) {
            FileOutputStream fileOutputStream;
            fileOutputStream = new FileOutputStream(outputFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream);
            tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        }
        for (String inputString : inputStringList) {
            MyLog.append(inputString);
            String relativeFilePath;
            String absoluteFilePath;
            if (inputIsRelative) {
                relativeFilePath = inputString;
                absoluteFilePath = relativeString + "/" + inputString;
            } else {
                absoluteFilePath = inputString;
                relativeFilePath = relativePath.toURI().relativize(new File(absoluteFilePath).toURI())
                        .getPath().replace("\\", "/");
            }
            File inputFile = new File(absoluteFilePath);
            MyLog.append("absolute Path : " + absoluteFilePath);
            MyLog.append("relative Path : " + relativeFilePath);
            TarArchiveEntry tarEntry = null;
            // if (Files.isSymbolicLink(inputFile.toPath())) {
            if (Files.isSymbolicLink(Paths.get(absoluteFilePath))) {
                // note: symlinks on windows have to be created by mklink command, not
                // by "ln -s target link" in GitBash, neither in Cygwin
                // note: I don't have privilege to run mklink on window
                //
                // C:\Users\hantian\testdir>mklink /D blink b
                // You do not have sufficient privilege to perform this operation.
                //
                // C:\Users\hantian\testdir>mklink alink.txt a.txt
                // You do not have sufficient privilege to perform this operation.
                MyLog.append("this is a sym link");
                // https://www.codota.com/code/java/methods/org.apache.commons.compress.archivers.tar.TarArchiveEntry/setLinkName
                tarEntry = new TarArchiveEntry(inputFile.toString(), TarConstants.LF_SYMLINK);
                tarEntry.setLinkName(relativeFilePath);
            } else {
                tarEntry = new TarArchiveEntry(inputFile, relativeFilePath);
                tarEntry.setSize(inputFile.length());
            }
            tarArchiveOutputStream.putArchiveEntry(tarEntry);
            tarArchiveOutputStream.write(IOUtils.toByteArray(new FileInputStream(inputFile)));
            tarArchiveOutputStream.closeArchiveEntry();
        }
        // need manually close because we will tar multiple times
        // tarArchiveOutputStream.close();
        return tarArchiveOutputStream;
    }

    public static void createTar(String outputString, String inputDirString) throws IOException {
        File outputFile = new File(outputString);
        File inputDirFile = new File(inputDirString);
        List<File> files = new ArrayList<File>(FileUtils.listFiles(new File(inputDirString),
                new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY));
        ArrayList<String> inputList = new ArrayList<String>();
        for (File inputFile : files) {
            String absoluteFilePath = inputFile.toString().replace("\\", "/");
            inputList.add(absoluteFilePath);
        }
        TarArchiveOutputStream tarArchiveOutputStream = createTar(outputString, inputDirString, inputList,
                false, null);
        tarArchiveOutputStream.close();
    }

    public static void main(String[] args) {
        int willdo = 1;
        try {
            // use absolute paths
            if (willdo == 0) {
                List<File> files = new ArrayList<File>(FileUtils.listFiles(new File("C:/Users/william/testdir"),
                        new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY));
                ArrayList<String> StringList = new ArrayList<String>();
                for (File file : files) {
                    StringList.add(file.getPath().replace("\\", "/"));
                }
                TarArchiveOutputStream tarArchiveOutputStream = createTar("c:/users/william/junk.tar",
                        "C:/Users/william/testdir", StringList, false, null);
                tarArchiveOutputStream.close();
            }
            // use relative path
            if (willdo == 0) {
                String[] relativePaths = { "a.txt", "b.txt", "c/d.txt" };
                ArrayList<String> StringList = new ArrayList<String>();
                for (String s : relativePaths) {
                    StringList.add(s);
                }
                TarArchiveOutputStream tarArchiveOutputStream = createTar("c:/users/william/junk2.tar",
                        "C:/Users/william/testdir", StringList, true, null);
                tarArchiveOutputStream.close();
            }
            // test 2nd function
            if (willdo == 0) {
                createTar("c:/users/william/junk3.tar", "C:/Users/william/testdir");
            }
            // test unTar
            if (willdo == 1) {
                String outputDir = "C:/Users/william/testuntar";
                FileUtils.cleanDirectory(new File(outputDir));
                List<File> outputFiles = unTar("C:/Users/william/junk.tar", outputDir);
                MyLog.append("outputFiles = " + MyGson.toJson(outputFiles));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ArchiveException e) {
            e.printStackTrace();
        }
    }

    // https://stackoverflow.com/questions/315618/how-do-i-extract-a-tar-file-in-java/7556307#7556307
    public static List<File> unTar(final String inputString, final String outputDirString)
            throws FileNotFoundException, IOException, ArchiveException {
        File inputFile = new File(inputString);
        File outputDir = new File(outputDirString);
        MyLog.append(String.format("Untaring %s to dir %s.", inputFile.getAbsolutePath(),
                outputDir.getAbsolutePath()));
        final List<File> untaredFiles = new LinkedList<File>();
        final InputStream is = new FileInputStream(inputFile);
        final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
                .createArchiveInputStream("tar", is);
        TarArchiveEntry entry = null;
        while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
            final File outputFile = new File(outputDir, entry.getName());
            MyLog.append(entry.getName());
            if (entry.isSymbolicLink()) {
                // in windows, by default, one cannot create symbolic link
                // https://stackoverflow.com/questions/8228030/getting-filesystemexception-a-required-privilege-is-not-held-by-the-client-usi
                // https://commons.apache.org/proper/commons-compress/apidocs/org/apache/commons/compress/archivers/tar/TarArchiveEntry.html
                String target = entry.getLinkName();
                String newLink = outputFile.getAbsolutePath();
                MyLog.append(String.format("Attempting to link %s to %s.", target, newLink));
                // if it should be a link, we try to create the link first.
                // if we cannot create link, we copy
                boolean success = true;
                try {
                    Files.createSymbolicLink(Paths.get(newLink), Paths.get(target));
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                    MyLog.append("failed to create sym link " + newLink + ". will resort to copy");
                }
                if (success) {
                    continue;
                }
            }
            if (entry.isDirectory()) {
                MyLog.append(String.format("Attempting to write output directory %s.",
                        outputFile.getAbsolutePath()));
                if (!outputFile.exists()) {
                    MyLog.append(String.format("Attempting to create output directory %s.",
                            outputFile.getAbsolutePath()));
                    if (!outputFile.mkdirs()) {
                        throw new IllegalStateException(
                                String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                    }
                }
            } else {
                File parent = outputFile.getParentFile();
                if (!parent.exists())
                    if (!parent.mkdirs()) {
                        throw new IllegalStateException(
                                String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                    }
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
            }
            untaredFiles.add(outputFile);
        }
        debInputStream.close();
        return untaredFiles;
    }
}

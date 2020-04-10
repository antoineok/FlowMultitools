package fr.flowarg.flowio;

import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.GZIPOutputStream;

public final class FileUtils
{
    @NotNull
    public static String getFileExtension(@NotNull final File file)
    {
        final String fileName = file.getName();
        final int dotIndex = fileName.lastIndexOf(46);
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    @NotNull
    public static String removeExtension(final String fileName)
    {
        if (fileName == null) {
            return "";
        }
        if (!getFileExtension(new File(fileName)).isEmpty()) {
            return fileName.substring(0, fileName.lastIndexOf(46));
        }
        return fileName;
    }

    public static void createFile(@NotNull final File file) throws IOException
    {
        if (!file.exists())
        {
            file.mkdirs();
            file.createNewFile();
        }
    }

    public static void saveFile(File file, String text) throws IOException
    {
        final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(text);
        writer.flush();
        writer.close();
    }

    @NotNull
    public static String loadFile(@NotNull final File file) throws IOException
    {
        if (file.exists())
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder text = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null)
            {
                text.append(line);
            }
            reader.close();
            return text.toString();
        }
        return "";
    }

    public static void deleteDirectory(@NotNull final File folder)
    {
        if (folder.exists() && folder.isDirectory())
        {
            final ArrayList<File> files = listFilesForFolder(folder);
            if (files.isEmpty()) {
                folder.delete();
                return;
            }
            for (final File f : files) {
                f.delete();
            }
            folder.delete();
        }
    }

    @NotNull
    public static ArrayList<File> listRecursive(@NotNull final File directory)
    {
        final ArrayList<File> files = new ArrayList<>();
        final File[] fs = directory.listFiles();
        if (fs == null) return files;

        for (final File f : fs)
        {
            if (f.isDirectory()) files.addAll(listRecursive(f));
            files.add(f);
        }
        return files;
    }

    public static void createDirectories(String location, @NotNull String... dirsToCreate) throws IOException
    {
        for (String s : dirsToCreate) {
            File f = new File(location, s);

            if (!f.exists()) Files.createDirectory(Paths.get(location + s));
        }
    }

    public static long getFileSizeMegaBytes(@NotNull File file)
    {
        return file.length() / (1024 * 1024);
    }
    public static long getFileSizeKiloBytes(@NotNull File file)
    {
        return  file.length() / 1024;
    }
    public static long getFileSizeBytes(@NotNull File file)
    {
        return file.length();
    }

    public static String getStringPathOfClass(@NotNull Class<?> classToGetPath)
    {
        return classToGetPath.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    @NotNull
    @Contract("_ -> new")
    public static File getFilePathOfClass(@NotNull Class<?> classToGetPath)
    {
        return new File(classToGetPath.getProtectionDomain().getCodeSource().getLocation().getPath());
    }

    @NotNull
    public static String getMD5FromURL(String input)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = new URL(input).openStream();

            try
            {
                is = new DigestInputStream(is, md);

                byte[] ignoredBuffer = new byte[8 * 1024];

                while (is.read(ignoredBuffer) > 0) ;

            }
            finally
            {
                is.close();
            }
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();

            for (byte b : digest)
            {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();

        } catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @NotNull
    public static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[1024];
        int bytesCount;

        while ((bytesCount = fis.read(byteArray)) != -1)
        {
            digest.update(byteArray, 0, bytesCount);
        }

        fis.close();

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes)
        {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    @NotNull
    public static String getMD5ofFile(final File file) throws NoSuchAlgorithmException, IOException
    {
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        return getFileChecksum(md5Digest, file);
    }

    public static void unzipJar(String destinationDir, String jarPath) throws IOException
    {
        File file = new File(jarPath);
        JarFile jar = new JarFile(file);

        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();)
        {
            JarEntry entry = enums.nextElement();

            String fileName = destinationDir + File.separator + entry.getName();
            File f = new File(fileName);

            if (fileName.endsWith("/")) f.mkdirs();
        }

        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();)
        {
            JarEntry entry = enums.nextElement();

            String fileName = destinationDir + File.separator + entry.getName();
            File f = new File(fileName);

            if (!fileName.endsWith("/"))
            {
                InputStream is = jar.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(f);

                while (is.available() > 0)
                {
                    fos.write(is.read());
                }

                fos.close();
                is.close();
            }
        }
    }

    public static void unzipJars(@NotNull JarPath... jars) throws IOException
    {
        for (JarPath jar : jars)
        {
            unzipJar(jar.getDestination(), jar.getJarPath());
        }
    }

    public static class JarPath implements Serializable
    {
        private String destination;
        private String jarPath;

        public JarPath(String destination, String jarPath)
        {
            this.destination = destination;
            this.jarPath = jarPath;
        }

        public String getDestination()
        {
            return destination;
        }

        public String getJarPath()
        {
            return jarPath;
        }
    }

    @Nullable
    public static String getSHA1(final File file)
    {
        try {
            try (InputStream input = new FileInputStream(file))
            {
                final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                final byte[] buffer = new byte[8192];
                for (int len = input.read(buffer); len != -1; len = input.read(buffer))
                    sha1.update(buffer, 0, len);
                return new HexBinaryAdapter().marshal(sha1.digest()).toLowerCase();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    public static ArrayList<File> listFilesForFolder(@NotNull final File folder)
    {
        final ArrayList<File> files = new ArrayList<>();
        File[] listFiles;
        for (int length = (listFiles = folder.listFiles()).length, i = 0; i < length; ++i)
        {
            assert listFiles != null;
            final File fileEntry = listFiles[i];
            if (fileEntry.isDirectory()) {
                files.addAll(listFilesForFolder(fileEntry));
            }
            files.add(fileEntry);
        }
        return files;
    }

    @NotNull
    public static File[] list(@NotNull final File dir)
    {
        File[] files = dir.listFiles();

        return files == null ? new File[0] : files;
    }

    public static void decompressTarArchive(final File tarGzFile, final File destinationDir)
    {
        final TarGZipUnArchiver unArchiver = new TarGZipUnArchiver();
        final ConsoleLoggerManager console = new ConsoleLoggerManager();
        console.initialize();
        unArchiver.enableLogging(console.getLoggerForComponent("[Launcher - Guns of Chickens]"));
        unArchiver.setSourceFile(tarGzFile);
        unArchiver.setDestDirectory(destinationDir);
        destinationDir.mkdirs();
        unArchiver.extract();
    }

    public static void gzipFile(String baseFile, String newFile) throws IOException
    {
        final byte[] buffer = new byte[1024];

        if(baseFile != null && newFile != null)
        {
            final FileOutputStream fileOutputStream = new FileOutputStream(newFile);
            final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
            final FileInputStream fileInputStream = new FileInputStream(baseFile);
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) > 0)
            {
                gzipOutputStream.write(buffer, 0, bytesRead);
            }

            fileInputStream.close();
            gzipOutputStream.finish();
            gzipOutputStream.close();
        }
    }
}
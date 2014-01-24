/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.flamefeed.ftb.modpackupdater;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JOptionPane;
import static org.apache.commons.io.FileUtils.iterateFiles;
import org.apache.commons.io.filefilter.TrueFileFilter;

/**
 * This class contains file utility methods to support the rest of the application.
 * For example it can find the Minecraft installation directory.
 * 
 * @author Francis
 */

public class FileOperator {
   
    /*
     * Constants
     */    
    
    // These are used while referencing the remoteHashes array
    private final int MD5HASH = 0;
    private final int FILENAME = 1;
    
    // Root of all remote downloads
    public final String REMOTE_FILES_LOCATION = "http://ftb.flamefeed.net/files";
    
    // Name of the hashfile on the remote server
    private final String HASHFILE_NAME = "hashfile.txt";    
        
    /*
     * Member variables
     */
    
    /*
     * These String array lists hold the relative paths of all files which should
     * be downloaded or deleted.
     */
    
    private List<String> downloadQueue;
    private List<String> deleteQueue;   

    /*
     * 2d array for file names and their respective hashes. Holds data extracted
     * from the remote hash-file.
     * 
     * This data includes all the file names which should be present inside the Minecraft
     * directory. MD5 hashes are included for each file in order to verify that
     * each local file matches its copy on the remote server. This is for files
     * with identical file names but potentially different versions.
     * 
     * A 2d array was chosen so that each file and hash could be referenced by
     * the same index. The inner array has 2 elements (filename and hash) while
     * the outer array has n elements, where n is the number of files on the remote
     * server.
     * 
     * File names are stored as relative paths, for example:
     * "mods/gregtechmod.zip" 
     */
    
    private String[][] remoteHashes;
    
    
    // Local file path to the vanilla Minecraft folder
    private String pathMinecraft;
 
    /*
     * Public class methods
     */
    
    /**
     * Constructor will perform tasks to initialise class member variables, such as
     * locating the local Minecraft folder, also downloading and parsing the hash-file.
     */
    
    public FileOperator() {        
        computePathMinecraft();
        
        try {
            parseHashfile();
        } catch(IOException ex) {
            System.err.println("Exception caught while parsing hashfile");
            System.err.println(ex.getMessage());
            
            JOptionPane.showMessageDialog(null, "Updater was unable to download and "
                    + "parse the remote hash-file.\nThis is probably because you are "
                    + "not connected to the internet,\nor the remote file server is down. "
                    + "Contact Sirloin if problems persist.");            
            
            System.exit(0);
        }
        
        try {
            compareLocalFiles();
        } catch(IOException | NoSuchAlgorithmException ex) {
            System.err.println("Exception caught while comparing local files");
            System.err.println(ex.getMessage());
            
            JOptionPane.showMessageDialog(null, "Updater had difficulties reading "
                    + "certain files on your\ncomputer. Ensure that this program has "
                    + "permission to read your\napplication data folder.");
            
            System.exit(0);
        }
        
        // Initialisation complete!
    }
    
    /**
     * Get an iterator that iterates through all entries on the download queue.
     * 
     * @return 
     * The ListIterator<String> which iterates through the download queue.
     */
    
    public ListIterator<String> getDownloadQueueIterator() {
        return downloadQueue.listIterator();
    }
    
    /**
     * Get an iterator that iterates through all entries on the delete queue.
     * 
     * @return 
     * The ListIterator<String> which iterates through the delete queue.
     */
    
    public ListIterator<String> getDeleteQueueIterator() {
        return deleteQueue.listIterator();
    }    
    
    /**
     * Obtains the Minecraft installation path from the FileOperator member
     * variable.
     * 
     * @return 
     * The Minecraft installation path
     */
    
    public String getMinecraftPath() {
        return pathMinecraft;
    }
    
    /**
     * Remove an element from the download queue
     * 
     * @param element 
     * The contents of the String element to remove.
     */
    
    public void removeFromDownloadQueue(String element) {
        downloadQueue.remove(element);
    }
    
    /**
     * Remove an element from the delete queue
     * 
     * @param element 
     * The contents of the String element to remove.
     */    
    
    public void removeFromDeleteQueue(String element) {
        deleteQueue.remove(element);
    }
    
    /**
     * Make sure all higher level directories are created before attempting to write to
     * a file.
     * 
     * @param path
     * Path must be the relative path of the file, for example:
     * "mods/gregtechmod.zip"
     */    
    
    public void createDirectories(String path) {
        /*
         * Split path into each of its directories.        
         */
        
        String[] directories = path.split("/");
        
        /*
         * The last element of directories will be the name of the file itself.
         * As we don't want to create a directory with this name, we use
         * x < directories.length - 1
         *
         * If the directory already exists, calling mkdir() will do nothing.
         */
        
        for(int x=0; x < (directories.length - 1); x++) {
            new File(pathMinecraft + "/" + directories[x]).mkdir();
        }        
    }
    
    /**
     * Download remote file and save output to file system. Called from constructor,
     * so final to prevent being overridden.
     * 
     * @param path
     * A relative path to the file which will be downloaded. This determines both
     * the target URL and the local destination path. Example:
     * "mods/gregtechmod.zip"
     * 
     * @throws java.io.IOException
     */
    
    public void downloadFile(String path) throws IOException {        
        URL urlRemoteTarget = new URL(REMOTE_FILES_LOCATION + "/" + path);
        ReadableByteChannel in = Channels.newChannel(urlRemoteTarget.openStream());
        FileOutputStream out = new FileOutputStream(pathMinecraft + "/" + path);
        out.getChannel().transferFrom(in, 0, Long.MAX_VALUE);        
    }    
    
    /**
     * Goes through every file in the deleteQueue and removes it from the file system
     */
    
    public void executeDeleteQueue() {
        ListIterator<String> deleteQueueIterator = deleteQueue.listIterator();
        
        while (deleteQueueIterator.hasNext()) {
            new File(pathMinecraft + "/" + deleteQueueIterator.next()).delete();
        }
    }
    
    /*
     * Private class methods
     */        
    
    /**
     * Obtain the absolute file path for the vanilla Minecraft directory based
     * on which Operating System is present
     */
    
    private void computePathMinecraft() {
        final String operatingSystem = System.getProperty("os.name");

        if (operatingSystem.contains("Windows") || operatingSystem.equals("Linux")) {
            pathMinecraft = System.getenv("appdata") + "/.minecraft";
        } else if (operatingSystem.contains("Mac")) {
            pathMinecraft = System.getenv("appdata") + "/minecraft";
        } else {
            pathMinecraft = System.getenv("appdata") + "/.minecraft";
            pathMinecraft = JOptionPane.showInputDialog(null,
                    "Operating system not recognised, please indicate installation path for vanilla Minecraft launcher.",
                    "Confirm Install Path",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    pathMinecraft).toString();
        }
        
        // Replace all \\ which might occur in Windows paths with / to make
        // it uniform. Sadly this requires a regex of \\, which as a string is \\\\.
        // Dear god.
        pathMinecraft = pathMinecraft.replaceAll("\\\\", "/");
        
        // Check validity of Minecraft directory
        File fileMinecraft = new File(pathMinecraft);
        
        if (pathMinecraft.equals("") ||
                (fileMinecraft.exists() && !fileMinecraft.isDirectory() ) ) {
            System.err.println("Invalid Minecraft installation path");
            System.err.println(pathMinecraft);
            JOptionPane.showMessageDialog(null, "Minecraft installation path is invalid. Installer will now terminate.");
            System.exit(0);
        }
        
        // Make sure Minecraft directory is present. Does nothing if already there.
        fileMinecraft.mkdir();
    }
    
    /**
     * This method will download the hash-file from the server and
     * will extract data from it and populate the remoteHashes 2d array.
     *
     * @throws java.lang.IOException
     */
    
    private void parseHashfile() throws IOException {
        int numRemoteFiles;  
        String[] currentLine;        
        FileReader fr;
        BufferedReader br;
        LineNumberReader lnr;
        
        downloadFile(HASHFILE_NAME);
        
        /*
         * Compute the number of lines in the hash-file. This corresponds to the
         * number of files on the remote server, and therefore the length of the 
         * hashFiles outer array.
         */

        fr = new FileReader(pathMinecraft + "/" + HASHFILE_NAME);
        lnr = new LineNumberReader(fr);
        lnr.skip(Long.MAX_VALUE);
        numRemoteFiles = lnr.getLineNumber();        
        fr.close();

        /*
         * Populate the remoteHashes[][] array with data from the hash file
         */

        remoteHashes = new String[numRemoteFiles][2];

        fr = new FileReader(pathMinecraft + "/" + HASHFILE_NAME);
        br = new BufferedReader(fr);

        /*
         * The hash-file format is as follows:
         * <MD5 hash><tab character><relative filename><newline character>
         * Splitting each line using a tab character "\t" separates the filename
         * from the MD5 hash.
         */

        for (int i = 0; i < numRemoteFiles; i++) {
            currentLine = br.readLine().split("\t", 2);
            remoteHashes[i][MD5HASH] = currentLine[MD5HASH];
            remoteHashes[i][FILENAME] = currentLine[FILENAME];                
        }

        fr.close();
    }
    
    /**
     * Generates an MD5 checksum of a file in hexadecimal form
     * 
     * @param path
     * Relative path to the file which will be checked, for example:
     * "mods/gregtechmod.zip"
     * 
     * @return
     * The resulting hexadecimal checksum as a string.
     */
    
    private String getMD5Checksum(String path) throws IOException, NoSuchAlgorithmException {
        // MessageDigest provides the functionality of the MD5 algorithm
        MessageDigest MD5Digest;
        FileInputStream fis;
        BufferedInputStream bis;
        byte[] data = new byte[1024];
        int bytesRead;
        
        MD5Digest = MessageDigest.getInstance("MD5");            

        fis = new FileInputStream(pathMinecraft + "/" + path);
        bis = new BufferedInputStream(fis, 1024);            

        do {
            bytesRead = bis.read(data, 0, 1024);

            if(bytesRead > 0) {
                MD5Digest.update(data, 0, bytesRead);
            }
        // BufferedInputStream.read() returns -1 at end of stream.
        } while(bytesRead != -1);

        // Present MD5 hash as a String
        data = MD5Digest.digest();                    
        String checksum = "";
        for(byte b : data) {
            // Prime example of write-only code
            checksum += Integer.toString((b & 255) + 256, 16).substring(1);
        }            
        
        return checksum;
    }
    
    /**
     * This method is used to compare local files against the server files.
     * Missing / outdated files are marked for download and excess files are
     * marked for deletion.
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    
    private void compareLocalFiles() throws IOException, NoSuchAlgorithmException {
        downloadQueue = new ArrayList<>();
        deleteQueue = new ArrayList<>();        
        
        /*
         * First check for files which are on the hash-list, but are not on the
         * local file system. Also check for local files whose MD5 hash does not
         * match the hash indicated in the hash-file.
         */
        
        for(String[] remoteHash : remoteHashes) {
            File currentFile = new File(pathMinecraft + "/" + remoteHash[FILENAME]);
                                    
            if(currentFile.exists()) {
                String localHash = getMD5Checksum(remoteHash[FILENAME]);
                if(! localHash.equals(remoteHash[MD5HASH])) {
                    downloadQueue.add(remoteHash[FILENAME]);
                }
            } else {
                downloadQueue.add(remoteHash[FILENAME]);
            }
        }
        
        /*
         * Next check for files which are present on the local file system, but are
         * not present in the hash-file.
         */
        
        findUnaccountedFiles("mods");
        findUnaccountedFiles("config");
    }
    
    /**
     * Recursively search for a directory and locate any files which aren't accounted
     * for on the hash-file. Add these files to the deleteQueue.
     * 
     * @param path
     * The relative path to search for unaccounted files.
     * 
     * @throws IOException 
     */
    
    private void findUnaccountedFiles(String path) throws IOException {
        File pathToSearch = new File(pathMinecraft + "/" + path);
        
        if (pathToSearch.exists() && pathToSearch.isDirectory()) {
            /* 
             * This iterator is provided by Apache commons.io and will recurse
             * over all files in given directory and also its subdirectories.
             */
            
            Iterator<File> fileList = iterateFiles(pathToSearch,
                    TrueFileFilter.INSTANCE,
                    TrueFileFilter.INSTANCE);
            
            while (fileList.hasNext()) {
                File currentFile = fileList.next();
                
                // Get the relative path of the file
                String relativePath = currentFile.getAbsolutePath();
                
                // Replace all \\ which might occur in Windows paths with / to make
                // it uniform. Sadly this requires a regex of \\, which as a string is \\\\.
                // Dear god.
                relativePath = relativePath.replaceAll("\\\\", "/");                
                relativePath = relativePath.replaceFirst(pathMinecraft + "/", "");
                
                boolean fileAccountedFor = false;
                
                for (String[] remoteHash : remoteHashes) {
                    if (remoteHash[FILENAME].equals(relativePath)) {
                        fileAccountedFor = true;
                        break;
                    }
                }
                
                if (!fileAccountedFor) {
                    deleteQueue.add(relativePath);
                }
            }
        }
    }
}
    


    
    
    




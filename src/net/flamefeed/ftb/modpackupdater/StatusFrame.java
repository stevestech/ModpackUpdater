/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.flamefeed.ftb.modpackupdater;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * This jFrame is used to display the current progress of file download operations.
 * All GUI update methods are run from the event dispatch thread, which is separate
 from the thread in which the program was started in. This is a feature which Java
 Swing requires to work properly.
 
 Actual file download and file delete operations are carried out in a background thread
 using a swing worker. This is so that the event dispatch thread does not get blocked by
 lengthy file downloads which could prevent the StatusFrame from being repainted.
 * 
 * @author Francis
 */
public class StatusFrame extends javax.swing.JFrame {    

    // This is for storing the instance of the FileOperator which was
    // initialised in the main thread
    private FileOperator fileOperator;
    
   /**
    * This inner class is used by FileWorker to pass multiple values to the Swing Worker's
    * process method. This enables the update of multiple progress bars, and the name
    * of the current task to be displayed in the GUI.
    * 
    * @author Francis
    */        
    //<editor-fold defaultstate="collapsed" desc="Definition of FileWorkerState inner class">

    private class FileWorkerState {
        private final int taskMaxValue;
        private final int taskValue;
        private final int overallMaxValue;
        private final int overallValue;
        private final String taskName;

        // Constructor to set just the task name jLabel, progress bars set to 0
        public FileWorkerState(String taskName) {
            taskMaxValue = 1;
            taskValue = 0;
            overallMaxValue = 1;
            overallValue = 0;
            this.taskName = taskName;
        }

        // Constructor to set progress bars and task name jLabel
        public FileWorkerState(int taskMaxValue,
                int taskValue,
                int overallMaxValue,
                int overallValue,
                String taskName) {

            this.taskMaxValue = taskMaxValue;
            this.taskValue = taskValue;
            this.overallMaxValue = overallMaxValue;
            this.overallValue = overallValue;
            this.taskName = taskName;
        }

        public int getTaskMaxValue() {
            return taskMaxValue;
        }

        public int getTaskValue() {
            return taskValue;
        }

        public int getOverallMaxValue() {
            return overallMaxValue;
        }

        public int getOverallValue() {
            return overallValue;
        }

        public String getTaskName() {
            return taskName;
        }
    }
    //</editor-fold>
    
    /**
     * This swing worker is used to execute all background file tasks such as deleting and
     * downloading files. By doing these tasks in a background thread, the event dispatch
     * thread remains unblocked and is able to continue updating the status window GUI.
     * 
     * @author Francis
     */
    //<editor-fold defaultstate="collapsed" desc="Definition of FileWorker inner class">    
    
    private class FileWorker extends SwingWorker<Void, FileWorkerState> {
        private final FileOperator fileOperator;
        

        // Private member variables for FileWorker inner class
        private int overallFileSize = 0;
        private int dataPreviouslyRead = 0;

        /**
         * Constructor to accept the instance of fileOperator that was initialised
         * in the main thread.
         * 
         * @param fileOperator 
         * A pre-initialised instance of the FileOperator class should be sent to
         * this constructor.
         */
        
        public FileWorker(FileOperator fileOperator) {
            // File worker needs to hold onto its own instance of fileOperator, as it will be
            // working in a background thread.            
            this.fileOperator = fileOperator;
        }
        
        /**
         * Retrieve the file size of all remote files on the markedForDownload list
         */

        private void computeOverallFileSize() throws IOException {
            ListIterator<String> downloadQueueIterator = fileOperator.getDownloadQueueIterator();

            while(downloadQueueIterator.hasNext()) {
                // Connect to the site using the URL object and determine file size
                URL url = new URL(fileOperator.REMOTE_FILES_LOCATION + "/" + downloadQueueIterator.next());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                overallFileSize += connection.getContentLength();
            }        
        }        

        /**
         * This method will download files from the remote server. After it has filled
         * its buffer of 1024 bytes, it will use the publish method to update the status
         * window progress bars and task name label.
         * 
         * @throws IOException
         */

        private void downloadFile(String path) throws IOException {                        
            String localFilePath = fileOperator.getMinecraftPath() + "/" + path;
            
            fileOperator.createDirectories(path);

            // Connect to the site using the URL object and determine file size
            URL url = new URL(fileOperator.REMOTE_FILES_LOCATION + "/" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int currentFileSize = connection.getContentLength();
            int currentDataRead = 0;

            java.io.BufferedInputStream in = new java.io.BufferedInputStream(connection.getInputStream());

            java.io.FileOutputStream fos = new java.io.FileOutputStream(localFilePath);

            java.io.BufferedOutputStream bout = new java.io.BufferedOutputStream(fos, 1024);

            byte[] data = new byte[1024];
            int i;

            // Fill the buffer with 1024 bytes from the buffered input stream
            while((i=in.read(data, 0, 1024)) >= 0) {
                currentDataRead += i;
                bout.write(data, 0, i);

                // Update the progress bars with the download progress
                publish(new FileWorkerState(currentFileSize,
                        currentDataRead,
                        overallFileSize,
                        (dataPreviouslyRead + currentDataRead),
                        path));                
            }

            bout.close();
            in.close();

            dataPreviouslyRead += currentDataRead;                          
        }

        /**
         * This is the code which is executed in the background thread.
         * It deletes and downloads the marked files from BeastUnleashedLauncher.
         */

        @Override
        protected Void doInBackground() {
            try {
                // Update the GUI to show that files are currently being deleted
                // Don't bother using the progress bars, this will be quick
                publish(new FileWorkerState("Deleting files..."));
                fileOperator.executeDeleteQueue();

                // Set the totalFileSize variable
                computeOverallFileSize();

                // Download files
                ListIterator<String> downloadQueueIterator = fileOperator.getDownloadQueueIterator();

                while(downloadQueueIterator.hasNext()) {
                    downloadFile( downloadQueueIterator.next() );
                }
            } catch(IOException ex) {
                System.err.println("Caught IOException while downloading files.");
                System.err.println(ex.getMessage());
                
                JOptionPane.showMessageDialog(null, "The mod-pack updater was unable to download files from the remote\n"
                    + "server. Please check your internet connection and report to Sirloin\n"
                    + "if this happens repeatedly");                
                
                System.exit(0);
            }
            
            return null;                
        } 

        /**
         * This code is executed in the event dispatch thread, and updates the progress
         * bars in StatusFrame with information from the background thread.
         */

        @Override
        protected void process(List<FileWorkerState> chunks) {
            // Retrieve the most recent state
            FileWorkerState mostRecentState = chunks.get(chunks.size()-1);

            // Update the progress bars and current task name
            updateCurrentTask( mostRecentState.getTaskName() );
            
            updateTaskProgress( mostRecentState.getTaskValue(),
                    mostRecentState.getTaskMaxValue() );
            
            updateOverallProgress( mostRecentState.getOverallValue(),
                    mostRecentState.getOverallMaxValue() );
        }
        
        /**
         * This code is executed in the event dispatch thread once the background
         * thread has completed the tasks set for it in doInBackground().
         */
        
        @Override
        protected void done() {            
            allTasksCompleted();                        
        }

    }
    //</editor-fold>
    
    /**
     * Constructor initialises all GUI components and schedules the FileWorker for
     * execution in a background thread, so that files may be downloaded and
     * deleted.
     * 
     * @param fileOperator
     * A pre-initialised instance of the FileOperator class should be sent to
     * this constructor.
     */
    
    public StatusFrame(FileOperator fileOperator) {
        this.fileOperator = fileOperator;
        initComponents();
        beginBackgroundTasks();
    }
    
    private void beginBackgroundTasks() {
        new FileWorker(fileOperator).execute();
    }
    
    public void updateCurrentTask(String newTask) {
        currentTaskLabel.setText(newTask);
    }
    
    public void updateTaskProgress(int value, int maxValue) {
        currentTaskProgressBar.setMaximum(maxValue);
        currentTaskProgressBar.setValue(value);
    }
    
    public void updateOverallProgress(int value, int maxValue) {
        overallProgressBar.setMaximum(maxValue);
        overallProgressBar.setValue(value);
        
        // To decipher the format string, see here:
        // http://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax
        // %1$.1f means parameter one should be formatted as float with 1 decimal place
        // %% is an escaped % sign
        this.setTitle(String.format("Progress (%1$.1f%%)", (float)value * 100 / maxValue));
    }    
    
    public void allTasksCompleted() {
        // Close StatusFrame
        this.dispose();
        
        JOptionPane.showMessageDialog(null, "Your mod-pack is now ready to use. Open the vanilla Minecraft launcher\n"
                    + "and select the FTB Unleashed profile to start up the mod-pack");
        
        // Terminate everything
        System.exit(0);
    }        

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        currentTaskLabel = new javax.swing.JLabel();
        currentTaskProgressBar = new javax.swing.JProgressBar();
        overallProgressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        jLabel1.setText("Current task:");

        currentTaskLabel.setText("(none)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(overallProgressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addComponent(currentTaskProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(currentTaskLabel))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(currentTaskLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(currentTaskProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(overallProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
   
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel currentTaskLabel;
    private javax.swing.JProgressBar currentTaskProgressBar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JProgressBar overallProgressBar;
    // End of variables declaration//GEN-END:variables
}

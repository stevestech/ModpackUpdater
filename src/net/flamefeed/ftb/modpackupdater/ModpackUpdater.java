/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package net.flamefeed.ftb.modpackupdater;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;



/**
 *
 * @author Francis
 */
public class ModpackUpdater {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // This class will manage all file related tasks. Constructor performs
        // program initialisation
        final FileOperator fileOperator = new FileOperator();                
                
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            System.err.println("Could not set look and feel to Nimbus.");
            System.err.println(ex.getMessage());
            
            JOptionPane.showMessageDialog(null, "Could not set look and feel to Nimbus");
            System.exit(0);
        }
        //</editor-fold>

        /* Program execution will now pass into the event dispatch thread, and the
         * current thread is now finished. Information currently stored in this
         * instance of fileOperator will now be passed to the event dispatch
         * thread via the TaskConfirmationFrame constructor.
         */

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TaskConfirmationFrame(fileOperator).setVisible(true);
            }
        });                                   
    }   
}

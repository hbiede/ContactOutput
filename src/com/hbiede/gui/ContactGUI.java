package com.hbiede.gui;

import com.hbiede.ContactPhotos;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class ContactGUI {
    public JPanel mainPanel;
    private JButton fileButton;
    private JButton directoryButton;
    private JButton runButton;
    private JProgressBar progressBar;
    private JPanel radioPanel;
    private JRadioButton firstLastRadioButton;
    private JRadioButton lastFirstRadioButton;
    private JLabel radioTitleLabel;
    private JLabel contactCountLabel;
    private ButtonGroup radioButtonGroup;
    private boolean outputNameLastFirst;
    private File inputFile;
    private File outputDirectory;


    public ContactGUI() {
        outputNameLastFirst = true;
        inputFile = outputDirectory = null;
        fileButton.addActionListener(new ButtonListener());
        directoryButton.addActionListener(new ButtonListener());
        runButton.addActionListener(new ButtonListener());
        firstLastRadioButton.addActionListener(new RadioListener());
        lastFirstRadioButton.addActionListener(new RadioListener());
    }

    private static int countContacts(File contactsFile) {
        BufferedReader br;
        int contactsCount = 0;
        try {
            br = new BufferedReader(new FileReader(contactsFile));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return 0;
        }

        String newLine;
        try {
            newLine = br.readLine();
            while (newLine != null) {
                if (newLine.contains("PHOTO;")) {
                    contactsCount++;
                }
                newLine = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            // Shouldn't ever come up. Just pleasing the compiler
            System.out.println("Well, that's a problem...");
            e.printStackTrace();
        }
        return contactsCount;
    }

    private class RadioListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            outputNameLastFirst = "LastFirst".equals(e.getActionCommand());
        }
    }

    private class ButtonListener implements ActionListener {

        // Called when a button is pressed
        @Override
        public void actionPerformed(ActionEvent e) {
            progressBar.setValue(0);
            if (e.getSource().equals(fileButton)) {
                // Open File selection window
                final JFileChooser fc = new JFileChooser();
                // Only allow vCards
                fc.setFileFilter(new FileNameExtensionFilter("vCard", "vcf"));
                // Only set the file location if it is an affirmative selection
                int returnVal = fc.showOpenDialog(ContactGUI.this.mainPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    inputFile = fc.getSelectedFile();

                    // Check if there is enough free space on the disk to store the photos
                    // Working under the assumption that inputFile's file size serves as a
                    if (inputFile.length() >= inputFile.getFreeSpace()) {
                        JOptionPane.showMessageDialog(new JFrame("Not Enough Space"), "Not Enough Free Space!\nPlease clear out some hard drive space.", "Dialog",
                                JOptionPane.ERROR_MESSAGE);
                        inputFile = null;
                    } else {
                        // Give a warning if the file is rather large (100MB+)
                        if (inputFile.length() > 0x6400000) {
                            contactCountLabel.setText("Loading...");
                            contactCountLabel.setVisible(true);
                            System.out.println("Big");
                        }
                        // Count the number of contacts in the file
                        progressBar.setMaximum(countContacts(inputFile));
                        contactCountLabel.setVisible(false);
                    }
                }
            } else if (e.getSource().equals(directoryButton)) {
                // Open Directory selection window
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                // Only set the file location if it is an affirmative selection
                int returnVal = fc.showOpenDialog(ContactGUI.this.mainPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    outputDirectory = fc.getSelectedFile();
                }
            } else {
                // Run Photo Output
                progressBar.setValue(0);
                ContactPhotos.outputPhotos(inputFile, outputDirectory, progressBar, outputNameLastFirst);
                // Display final contact count
                contactCountLabel.setText(String.format("%d contact photos exported", progressBar.getValue()));
                contactCountLabel.setVisible(true);
            }

            // Enable the run button once there are valid selections
            runButton.setEnabled(inputFile != null && outputDirectory != null);
        }
    }
}

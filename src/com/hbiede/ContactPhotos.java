package com.hbiede;
import com.hbiede.gui.ContactGUI;

import java.io.*;
import java.net.URL;
import java.util.Base64;

import javax.swing.*;

public class ContactPhotos {

	public static void main(String args[]){
		JFrame frame = new JFrame("Contact Photos");
		frame.setContentPane(new ContactGUI().mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	public static void outputPhotos(File inputFile, File outputDirectory, JProgressBar progressBar, boolean outputNameLastFirst) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.out.println("Please input a valid vCard file.");
			return;
		}

		String newLine;
		try {
			newLine = br.readLine();
			String contactName = "";
			String photoString = "";
			boolean photoInProgress = false;

			// Create the output folder if it does not exist
			outputDirectory.mkdir();
			while (newLine != null) {
				if (newLine.startsWith("FN:")) {
					// Get the name for a new Contact
					if (outputNameLastFirst) {
						String[] tokens = newLine.substring(3).split(" ", 2);
						if (tokens.length > 1) {
							contactName = tokens[1] + ", " + tokens[0];
						} else {
							contactName = tokens[0];
						}
					} else {
						contactName = newLine.substring(3);
					}
				} else if (newLine.startsWith("ORG:")) {
					contactName = newLine.substring(4).split(";")[0];
				} else if (newLine.contains("PHOTO")) {
					// Start a new photo string
					photoString = newLine.substring(newLine.indexOf(":") + 1);
					photoInProgress = true;
				} else if (photoInProgress && newLine.charAt(0) == ' ') {
					// Add to the existing photo string if the line starts with a space (an indent
					// to signify the photo is continuing)
					photoString += newLine.substring(1);
				} else if (photoInProgress) {
					// Finish a photo string if the line is not indented while a photo string is
					// being built
					photoInProgress = false;
					try {
						File outputFile = new File(outputDirectory.getAbsolutePath() + "/" + contactName + ".jpg");
						OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
						if (photoString.contains("http")) {
							InputStream imageReader = new BufferedInputStream(new URL(photoString).openStream());
							int imageByte;
							while ((imageByte = imageReader.read()) != -1)
							{
								os.write(imageByte);
							}
							imageReader.close();
						} else {
							// Decode and store the output to the output folder created earlier
							os.write(Base64.getDecoder().decode(photoString));
						}
						SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
						os.close();
					} catch (Exception e) {
						System.out.printf("Broken photo on contact \"%s\"\n", contactName);
						continue;
					}

				}
				newLine = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.out.println("Well, that's a problem...");
			e.printStackTrace();
		}
	}
}

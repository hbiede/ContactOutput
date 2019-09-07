package com.hbiede;
import com.hbiede.gui.ContactGUI;

import java.io.*;
import java.net.URL;
import java.util.Base64;

import javax.swing.*;

public class ContactPhotos extends SwingWorker<Void, Void> {

	private File inputFile;
	private File outputDirectory;
	private JButton runButton;
	private int totalContacts;
	private int contactsOutputSoFar = 0;
	private boolean outputNameLastFirst = true;


	public static void main(String args[]){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Contact Photos");
				frame.setContentPane(new ContactGUI().mainPanel);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.pack();
				frame.setVisible(true);
			}
		});
	}

	public void setRunButton(JButton runButton) {
		this.runButton = runButton;
	}

	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public int getContactsOutputSoFar() {
		return contactsOutputSoFar;
	}

	public void setOutputNameLastFirst(boolean outputNameLastFirst) {
		this.outputNameLastFirst = outputNameLastFirst;
	}

	public void setTotalContacts(int totalContacts) {
		this.totalContacts = totalContacts;
	}

	public int getTotalContacts() {
		return totalContacts;
	}

	@Override
	protected Void doInBackground() throws Exception {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.out.println("Please input a valid vCard file.");
			return null;
		}

		String newLine;
		try {
			newLine = br.readLine();
			String contactName = "";
			String photoString = "";
			boolean photoInProgress = false;

			// Create the output folder if it does not exist
			outputDirectory.mkdir();
			while (newLine != null && !isCancelled()) {
				if (newLine.startsWith("N:")) {
					// Get the name for a new Contact
					if (outputNameLastFirst) {
						String[] tokens = newLine.substring(2).split(";");
						if (tokens.length > 1) {
							contactName = tokens[0] + ", " + tokens[1];
						} else if (tokens.length == 1) {
							contactName = tokens[0];
						}
					} else {
						contactName = newLine.substring(3);
					}
				} else if (newLine.startsWith("ORG:") && "".equals(contactName)) { // contact name must be blank so as to prevent a person with a company name associated from being named after the company
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
						contactsOutputSoFar++;
						setProgress(100 * contactsOutputSoFar / totalContacts);
						os.close();
						contactName = "";
					} catch (Exception e) {
						System.out.printf("Broken photo on contact \"%s\"\n", contactName);
						System.err.println(e.getMessage());
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
		return null;
	}

	@Override
	protected void done() {
		runButton.setActionCommand("run");
		runButton.setText("Run");
	}
}

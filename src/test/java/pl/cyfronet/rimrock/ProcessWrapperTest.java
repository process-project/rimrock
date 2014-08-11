package pl.cyfronet.rimrock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ProcessWrapperTest {
	@Test
	public void testProcessWrapper() throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder("/usr/bin/python", "-i");
		builder.redirectErrorStream(true);
		Process process = builder.start();
		
		new Thread(() -> {
			System.out.println("Output thread started");
			
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line = null;
				
				while((line = reader.readLine()) != null) {
					System.out.println("python: " + line);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Output thread finished");
		}).start();
		new Thread(() -> {
			System.out.println("Input thread started");
			Scanner scanner = new Scanner(System.in);
			String line = null;
			PrintStream out = new PrintStream(process.getOutputStream(), true);
			
			do {
				line = scanner.nextLine();
				
				try {
					System.out.println("Feeding next line: " + line);
					out.println(line);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while(!line.equals("exit()"));
			
			scanner.close();
			System.out.println("Input thread finished");
		}).start();
		process.waitFor();
	}
}
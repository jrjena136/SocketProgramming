package com.zimbra.assignment;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class SimpleHttpServer implements Runnable{
	
	static final File DEFAULT_FILE_PATH = new File("C:/Users/IBM_ADMIN/Desktop/JYOTI_Workspace/Eclipse_Neon/SimpleJavaHttpServer/src");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "method_not_supported.html";
	
	static int port = 8000;
	static String path = null;
	static final boolean verbose = true;
	private Socket socket;
	
	public SimpleHttpServer(Socket s) {
		this.socket = s;
	}
	
	public static void main(String[] args) {
		if(args.length>0){
			port = Integer.parseInt(args[0]);
			path = args[1];
		}
		else{
			throw new RuntimeException("pass atleat one value");
		}
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			System.out.println("Server started\nListening for connections on port: " + port+"...\n");
			while(true){
				SimpleHttpServer myServer = new SimpleHttpServer(serverSocket.accept());
				if(verbose){
					System.out.println("Connection opend - "+ new Date());
				}
				Thread thread = new Thread(myServer);
				thread.start();
			}
		} catch (IOException e) {
			System.err.println("Server connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		BufferedReader br = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		FileOutputStream fos = null;
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
			dataOut = new BufferedOutputStream(socket.getOutputStream());
			String input = br.readLine();
			System.out.println("Header details : " + input);
			StringTokenizer parser = new StringTokenizer(input);
			String method = parser.nextToken().toUpperCase();
			fileRequested = parser.nextToken().toLowerCase();
			if(method.equals("POST")){
				System.out.println("Post method processing start");
				input = "";
	            // looks for post data
	            int postDataI = -1;
	            while ((input = br.readLine()) != null && (input.length() != 0)) {
	                if (input.indexOf("Content-Length:") > -1) {
	                    postDataI = new Integer(
	                            input.substring(
	                                    input.indexOf("Content-Length:") + 16,
	                                    input.length())).intValue();
	                }
	            }
	            String postData = "";
	            // read the post data
	            if (postDataI > 0) {
	                char[] charArray = new char[postDataI];
	                br.read(charArray, 0, postDataI);
	                postData = new String(charArray);
	            }
	            System.out.println("Post body data : " + postData);
	            File file = new File(DEFAULT_FILE_PATH,path);
	            File writeFile = null;
				if(file.isDirectory()){
					System.out.println("if block");
					if(fileRequested.length()==0 || fileRequested.length()==1){
						writeFile= new File(DEFAULT_FILE_PATH,path+"/data.txt");
					}
					else{
						writeFile= new File(DEFAULT_FILE_PATH,path+fileRequested);
					}
					
					fos = new FileOutputStream(writeFile);
					fos.write(postData.getBytes());
				}
				else{
					System.out.println("else block");
					boolean created = file.mkdir();
					if(created){
						if(fileRequested.length()==0 || fileRequested.length()==1){
							writeFile = new File(DEFAULT_FILE_PATH,path+"/data.txt");
						}else{
							writeFile = new File(DEFAULT_FILE_PATH,path+fileRequested);
						}
						fos = new FileOutputStream(writeFile);
						fos.write(postData.getBytes());
					}
				}
				
				String response = "your message has been successfully written to file.";
				String content = "text/plain";
				out.println("HTTP/1.1 200 OK");
				out.println("Content-type:"+content+";charset=utf-8");
				out.println("Server : Java HTTP Server");
				out.println("Date: "+new Date());
				out.println("Content-Length:"+response.getBytes().length);
				out.println();
				out.flush();
				
				dataOut.write(response.getBytes());
				dataOut.flush();
				
				
			}
			else if(method.equals("GET") || method.equals("HEAD")){
				if(fileRequested.endsWith("/")){
					fileRequested+=DEFAULT_FILE;
				}
				File file = new File(DEFAULT_FILE_PATH,fileRequested);
				int fileLength = (int)file.length();
				String content = getContentType(fileRequested);
				if(method.equals("GET")){
					byte[] fileData = readFileData(file, fileLength);
					out.println("HTTP/1.0 200 OK");
					out.println("Server : Java HTTP Server from RJ");
					out.print("Date: "+new Date());
					out.println("Content-type:"+content);
					out.println("Content-length:"+fileLength);
					out.println();
					out.flush();
					dataOut.write(fileData,0,fileLength);
					dataOut.flush();
				}
				if(verbose){
					System.out.println("File "+fileRequested+" of type "+content+" returned");
				}
			}
			else{
				if(verbose){
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				File file = new File(DEFAULT_FILE_PATH, METHOD_NOT_SUPPORTED);
				int fileLength = (int)file.length();
				String contentMimeType = "text/html";
				byte[] fileData = readFileData(file,fileLength);
				
				out.println("HTTP/1.0 501 Not Implemented");
				out.println("Server : Java HTTP Server from RJ");
				out.print("Date: "+new Date());
				out.println("Content-type:"+contentMimeType);
				out.println("Content-length:"+file.length());
				out.println();
				out.flush();
				dataOut.write(fileData,0,fileLength);
				dataOut.flush();
			}
		}catch(FileNotFoundException fnfe){
			try{
				fileNotFound(out,dataOut,fileRequested);
			}catch(IOException ioe){
				System.err.println("Error with File not found ex: "+ ioe.getMessage());
			}
		}
		
		catch (IOException e) {
			System.err.println("Server error : " + e.getMessage());
		}
		finally {
			try {
				br.close();
				out.close();
				dataOut.close();
				fos.close();
				socket.close();
			} catch (Exception e) {
				System.err.println("Error closing stream: "+e.getMessage());
			}
			if(verbose){
				System.out.println("Connection closed.\n");
			}
		}
		
	}

	private void fileNotFound(PrintWriter out, BufferedOutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(DEFAULT_FILE_PATH,fileRequested);
		int fileLength = (int)file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.0 404 Not Found");
		out.println("Server : Java HTTP Server from RJ");
		out.print("Date: "+new Date());
		out.println("Content-type:"+content);
		out.println("Content-length:"+fileLength);
		out.println();
		out.flush();
		dataOut.write(fileData,0,fileLength);
		dataOut.flush();
		if(verbose){
			System.out.println("File " + fileRequested + " not found");
		}
		
		
	}

	private String getContentType(String fileRequested) {
		if(fileRequested.endsWith("html") || fileRequested.endsWith("htm")){
			return "text/html";
		}
		return "text/plain";
	}

	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fis = null;
		byte[] fileData = new byte[fileLength];
		fis = new FileInputStream(file);
		try {
			fis.read(fileData);
		} finally{
			if(fis!=null){
				fis.close();
			}
		}
		return fileData;
	}

}

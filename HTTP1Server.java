import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.nio.file.*;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.Arrays;

public class HTTP1Server{
	public static int port;
	public static int threadNum = 0;
	private static int threadCount = 0;
	
	public static void main(String[] args) throws Exception{
		if(args.length!=1){ /*checks to see if the user included the correct args*/
			System.err.println("Please include a port number... for example java - cp . PartialHTTP1Server 3456");
		return;
		}
	
		String possiblePort = args[0]; /*checks to see if the port number is an int*/
		for(int i=0; i<possiblePort.length();i++){
			if(possiblePort.charAt(i)>='0'&&possiblePort.charAt(i)<='9'){
			}else{
				System.err.println("Please enter a valid port number. Port number must be integers.");
				return;
			}
		}
		port = Integer.parseInt(args[0]);
		System.out.println("port: " + port);

		ServerSocket welcomeSocket = makeServer(port); /*calls makeServer to set up the server socket*/
		if(welcomeSocket == null){
			return;
		}
		welcomeSocket.close();
		return;
	}

	public static ServerSocket makeServer (int portNumber) throws Exception{
		int port = portNumber; /*sets up the server socket*/
		ServerSocket welcomeServer = null;
		boolean listen = true;
		Socket connectionSocket = null;
		int numOfThreads = 0;
		boolean maxThreads = false;
		int peakI = 0;
		try{
			welcomeServer = new ServerSocket(port);
			while (listen){
				connectionSocket = welcomeServer.accept(); /*when a client connects we spawn a new thread*/
				increase();
				peakI = peak();
				if ( peakI > 50){
					maxThreads = true;
					portThread pThread = new portThread(port,numOfThreads,connectionSocket,maxThreads);
					System.out.println("NUMBER OF THREADS IS: " + numOfThreads);
					pThread.start();
				
				}else{
					System.out.println("NUMBER OF THREADS IS: " + numOfThreads);
					maxThreads = false;
					portThread pThread = new portThread(port,numOfThreads,connectionSocket,maxThreads);
					pThread.start();
				}
			}
		}catch(Exception e){
			System.out.println(e);
			return null;
		}
		connectionSocket.close();
		return welcomeServer;
	}
	public static synchronized void increase(){
		threadCount++;
	}
	public static synchronized void decrease(){
		threadCount--;
	}
	public static synchronized int peak(){
		return threadCount;
	}
}

class portThread implements Runnable{
	private Thread t;
	private int port;
	private String threadName;
	private int threadNum;
	private Socket connectionSocket;
	private boolean maxThreads;

	portThread(int portNum,int threadN, Socket cSocket,boolean maxT){ /*constructs the thread*/
		threadNum = threadN;
		threadName = "thread " + Integer.toString(threadNum);
		port = portNum;
		maxThreads = maxT;
		connectionSocket = cSocket;
		System.out.println("Creating... " + threadName + " on port... " + port);
	}	

	public void run(){
	
		String fromClient =null;
		BufferedReader inFromClient = null;
		DataOutputStream outToClient = null;
		int stringSize;
		int error = 0;

		System.out.println("running the new thread"); /*START PROJECT HERE*/
		System.out.println("Max T is: " + maxThreads);
		if(maxThreads == true){
			try{
				System.out.println("Service Unavailable");   /* Server unavailable*/
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				outToClient.writeBytes("HTTP/1.0 503 Service Unavailable");
				outToClient.flush();
				t.sleep(500);
				try{
					outToClient.close();
					HTTP1Server.decrease();
					return;
				}catch(Exception k){
					System.err.println(k);
					HTTP1Server.decrease();
					return;
				}
			}catch(Exception j){
				System.err.println(j);
				HTTP1Server.decrease();
				return;
			}
		}

		try{
			connectionSocket.setSoTimeout(3000); /*sets the 408 timeout to 3 seconds*/  /*the time out is 30 seconds while i work on the project, change back soon*/
		}catch(Exception e){
			System.err.println("Could not set Timeout" + e);
			try{
				System.out.println("Internal Service Error");
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				outToClient.writeBytes("HTTP/1.0 500 Internal Service Error");
				outToClient.flush();
				t.sleep(500);
				try{
					outToClient.close();
					HTTP1Server.decrease();
					return;
				}catch(Exception k){
					System.err.println(k);
					HTTP1Server.decrease();
					return;
				}
			}catch(Exception j){
				System.err.println(j);
			}

		}
		String headerInfo = "";
		String payload = "";
		String addToHeader = "";
		int countHead = 1;
		List<Byte> bytes = new ArrayList<Byte>();
		String addToPayload = "";
		byte[] byteArray = null;
		int dataStream =0;
		try{
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); /*gets the input from the client*/
			fromClient = inFromClient.readLine();
			System.out.println("before while loop");
			while(!(addToHeader=inFromClient.readLine()).isEmpty()){
				System.out.println("top while loop");
				if(countHead ==1){
					headerInfo= headerInfo + addToHeader;
					countHead++;
				}else{
					headerInfo = headerInfo + '\n' + addToHeader;
					countHead++;
				}
			}
			}catch(Exception e){
			System.out.println("timeout occurred");
			try{
				outToClient = new DataOutputStream(connectionSocket.getOutputStream()); /*timeout occurs*/
				outToClient.writeBytes("HTTP/1.0 408 Request Timeout");
				outToClient.flush();
				t.sleep(500);
				System.out.println("408");
				try{
					inFromClient.close();		/*close connection*/
					outToClient.close();
					HTTP1Server.decrease();
					return;
				}catch(Exception g){
					System.err.println(g);
					HTTP1Server.decrease();
					return;
				}
			}catch(Exception f){
				System.err.println(f);
				error = 1;
			}
		}
		
		System.out.println("fromClient = " + fromClient);
		System.out.println("headerInfo = " + headerInfo);
		headerInfo = headerInfo + '\n';
		String line1 = "";
		String line2 = "";
		String line3 = "";
		String line4 = "";
		String line5 = "";
		int before = 0;
		String from = "";
		String userAgent = "";
		String contentType = "";
		String contentLength = "";
		String sF = "From:";
		String sU = "User-Agent:";
		String sCT = "Content-Type:";
		String sCL = "Content-Length:";

		if(countHead >1){ /*fills in the lines of the headers*/
			for(int qq = 0; qq<headerInfo.length();qq++){
				if(headerInfo.charAt(qq)=='\n'){
					if(line1==""){
						line1= headerInfo.substring(before,qq);
						before =qq;
					}else if(line2==""){
						line2=headerInfo.substring(before+1,qq);
						before = qq;
					}else if(line3 ==""){
						line3 = headerInfo.substring(before+1,qq);
						before = qq;
					}else if(line4 ==""){
						line4 = headerInfo.substring(before+1,qq);			
						before = qq;
					}else{
						line5 = headerInfo.substring(before+1,headerInfo.length()-1);
					}
				}
			}
		}
		System.out.println("line1: " + line1);
		System.out.println("line2: " + line2);
		System.out.println("line3: " + line3);
		System.out.println("line4: " + line4);
		System.out.println("line5: " + line5);
		boolean headerError = false;
		if(line1!=""){ /*parses out each line*/
			for(int qt = 0; qt<line1.length();qt++){
				if(line1.charAt(qt)==':'){
					System.out.println("sub of line 1 is: " + line1.substring(0,qt));
					if((line1.substring(0,qt+1)).compareTo(sF)==0){
						from = line1;
					}else if((line1.substring(0,qt+1)).compareTo(sU)==0){
						userAgent = line1;
					}else if((line1.substring(0,qt+1)).compareTo(sCT)==0){
						contentType = line1;
					}else if((line1.substring(0,qt+1)).compareTo(sCL)==0){
						contentLength = line1;
					}else{
						headerError = true;
					}	
				}
			}
		}
		if(line2!=""){
			for(int qt2 = 0; qt2<line2.length();qt2++){
				if(line2.charAt(qt2)==':'){
					if((line2.substring(0,qt2+1)).compareTo(sF)==0){
						from = line2;
					}else if((line2.substring(0,qt2+1)).compareTo(sU)==0){
						userAgent = line2;
					}else if((line2.substring(0,qt2+1)).compareTo(sCT)==0){
						contentType = line2;
					}else if((line2.substring(0,qt2+1)).compareTo(sCL)==0){
						contentLength = line2;
					}else{
						headerError = true;
					}	
				}
			}
		}
		if(line3!=""){
			for(int qt3 = 0; qt3<line3.length();qt3++){
				if(line3.charAt(qt3)==':'){
					if((line3.substring(0,qt3+1)).compareTo(sF)==0){
						from = line3;
					}else if((line3.substring(0,qt3+1)).compareTo(sU)==0){
						userAgent = line3;
					}else if((line3.substring(0,qt3+1)).compareTo(sCT)==0){
						contentType = line3;
					}else if((line3.substring(0,qt3+1)).compareTo(sCL)==0){
						contentLength = line3;
					}else{
						headerError = true;
					}	
				}
			}
		}
		if(line4!=""){
			for(int qt4 = 0; qt4<line4.length();qt4++){
				if(line4.charAt(qt4)==':'){
					if((line4.substring(0,qt4+1)).compareTo(sF)==0){
						from = line4;
					}else if((line4.substring(0,qt4+1)).compareTo(sU)==0){
						userAgent = line4;
					}else if((line4.substring(0,qt4+1)).compareTo(sCT)==0){
						contentType = line4;
					}else if((line4.substring(0,qt4+1)).compareTo(sCL)==0){
						contentLength = line4;
					}else{
						headerError = true;
					}	
				}
			}
		}
		System.out.println("from is: " + from);
		System.out.println("userAgent is: " + userAgent);
		System.out.println("contentType is: "+contentType);
		System.out.println("contentLength is: " + contentLength);
		String cLength = "";
		int cLen = 0;
		boolean isLenGood = true;
		InputStream inputStream = null;
		byte[] payloadBytes = null;
		try{
			inputStream = connectionSocket.getInputStream();		
			if(contentLength!=""){
				cLength = contentLength.substring(16,contentLength.length());
				for(int aqs=0; aqs<cLength.length();aqs++){
					if(cLength.charAt(aqs)>='0'&&cLength.charAt(aqs)<='9'){
					}else{
						isLenGood = false;
					}
				}
				System.out.println("cLength is: " + cLength);
				if(isLenGood==true){
					cLen = Integer.parseInt(cLength);
				}	
			}
			char temp1;
			System.out.print("things are about to get slow");	
			if(cLen>0){
			
				System.out.print("1");	
				payload =inFromClient.readLine();
			}
		}catch(Exception e){
			System.err.println(e);
		}	
		System.out.println("payload is: "+payload);
		int i = 0;
		stringSize = fromClient.length();
		int countj = 0;
		int countl = 0;
		int countb = 0;
		int badInput = 0;
		String resource = null;
		String resourceF = null;

		for(int b = 0; b<stringSize; b++){		/*starts to figure out if the input is bad*/
			if(fromClient.charAt(b)==' '){
				countb++;
			}else{
			}
		}
		if(countb != 2){
			badInput = 1;
		}
		if(badInput == 1){
			try{
				outToClient = new DataOutputStream(connectionSocket.getOutputStream()); /*if there are not enough spaces we bad request and close*/
				outToClient.writeBytes("HTTP/1.0 400 Bad Request");
				System.out.println("400");
				outToClient.flush();
				t.sleep(500);
			}catch(Exception e){
				System.err.println(e);
			}
			try{
				inFromClient.close();
				outToClient.close();
				HTTP1Server.decrease();
				return;
			}catch(Exception g){
				System.err.println(g);
				HTTP1Server.decrease();
				return;
			}

		}
	
		for(int j = 0; j<stringSize; j++){
			if(fromClient.charAt(j)!= ' '){
				countj++;
			}else{
				resource = fromClient.substring(countj+1,stringSize);
				break;
			}
		}
		String resourceP = "";
		for(int l = 0; l<resource.length(); l++){		/*gets the resourceF chunk "hell.html"*/
			if(resource.charAt(l)!= ' '){
				countl++;
			}else{
				resourceF = resource.substring(1,countl);
				resourceP = resource.substring(0,countl);
				break;
			}
		}
		
		String text = null;
		BufferedReader reader = null;
		String textInFile = null;
		int countk =0;
		String command = null;
		for(int k = 0; k<fromClient.length();k++){ 		/*gets the command chunk "POST"*/
			if(fromClient.charAt(k)!=' '){
				countk++;			
			}else{
				command = fromClient.substring(0,countk);
				break;
			}
		}
		int countSpace = 0;
		String type = null;
		for(int m = 0; m<stringSize; m++){			/*gets the type chunk "HTTP/1.0"*/
			if(fromClient.charAt(m)==' '){
				countSpace++;
			}else{
			}
			if(countSpace==2){
				type = fromClient.substring(m+1,stringSize);
				break;
			}
		}

		
		String extension = null;
		String extensionType = null;	
		int countS=0;
		int periodCount=0;
		for(int z = 0; z<resourceF.length(); z++){

			if(resourceF.charAt(z)=='.'){
				periodCount++;
			}
		}

		if(periodCount==1){
			for(int s = 0; s<resourceF.length(); s++){ /*helps figure out extension*/
				if (resourceF.charAt(s)!= '.'){
					countS++;
				}else{
					extension = resourceF.substring(countS,resourceF.length());
				}

			}
		}else if(periodCount ==0){
			extension ="none";


		}else{
			try{
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				outToClient.writeBytes("HTTP/1.0 400 Bad Request");
				System.out.println("400");
				outToClient.flush();
				t.sleep(500);
				try{
					inFromClient.close();		/*close the streams*/
					outToClient.close();
					HTTP1Server.decrease();
					return;
				}catch(Exception e){
					System.out.println(e);
					HTTP1Server.decrease();
					return;
				}
			}catch(Exception fe){
				System.out.println(fe);
			}
		}
		if(extension.compareTo(".txt") ==0){ /*figures out the mime type*/
			extensionType = "text/plain";
		}else if(extension.compareTo(".html")==0){
			extensionType = "text/html";
		}else if(extension.compareTo(".gif")==0){
			extensionType = "image/gif";
		}else if(extension.compareTo(".jpeg")==0){
			extensionType = "image/jpeg";
		}else if(extension.compareTo(".png")==0){
			extensionType = "image/png";
		}else if(extension.compareTo(".pdf")==0){
			extensionType = "application/pdf";
		}else if(extension.compareTo(".x-gzip")==0){
			extensionType = "application/x-gzip";
		}else if(extension.compareTo(".zip")==0){
			extensionType = "application/zip";
		}else{
			extensionType = "application/octet-stream";
		}
	

		int countA = 0;
		String version = null;
		for(int a = 0; a<type.length();a++){
			if(type.charAt(a)!='/'){
				countA++;
			}else{
				version = type.substring(countA+1,type.length());
				break;
			}
		}
		double versionDub = Double.parseDouble(version);	


		System.out.println("Command: " + command);
		System.out.println("Resource: " + resource);
		System.out.println("ResourceF: " + resourceF);
		System.out.println("Type: " + type);
		System.out.println("Version: " + version);
		System.out.println("versionDub: " + versionDub);
		System.out.println("Extension: " + extension);
		System.out.println("ExtensionType: " + extensionType);


		String post = "POST";
		String head = "HEAD";
		String http = "HTTP/1.0";
		String get = "GET";
		
		File f = new File(resourceF);
		boolean canReadFile = false;
		if(f.canRead()==true){
			canReadFile = true;
		}else{
			canReadFile = false;
		}
		boolean canExecuteFile = false;
		if(f.canExecute()==true){
			canExecuteFile = true;
		}else{
			canExecuteFile = false;
		}
		System.out.println("canExecuteFile = " + canExecuteFile);
		System.out.println("canReadFile = "+canReadFile);	
		BufferedReader reader1 = null;
		String textInFile1 = null;
		String text1 = null;
		int textLength=0;
		error = 0;
		BufferedReader reader2 = null;
		String textInFile2 = "";
		String text2 = null;					
		
		SimpleDateFormat sdf  = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z"); /*formats the date*/
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		String currDate = sdf.format(c.getTime());
		c.add(Calendar.DATE,3);
		String dateOutput = sdf.format(c.getTime());
		System.out.println("dateOutput: " + dateOutput);
		String dateOutput2 = null;
		if(f.exists() && !f.isDirectory()){
			SimpleDateFormat sdf2 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
			sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
			dateOutput2 = sdf2.format(f.lastModified());
		}	
		Path path = null;
		try{
			path = Paths.get(resourceF);
		}catch(Exception e){
			System.err.println(e);
		}

		Date date4 = null;
		Date date3 = null;
		boolean modified = true;		

		if(headerInfo.length()>35&&countHead==1){
			System.out.println("headerInfoSub: " + headerInfo.substring(0,18)); /*helps to format the date*/
			if(headerInfo.substring(0,18).compareTo("If-Modified-Since:")==0){
				System.out.println("substring: " + headerInfo.substring(19,headerInfo.length()));
				System.out.println("dateOutput2: " + dateOutput2);

				try{
					SimpleDateFormat sdf3 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
					date3 = sdf3.parse(headerInfo.substring(19,headerInfo.length()));
					System.out.println("Date 3 = " + date3);
				}catch(Exception e){
					System.err.println(e);
				}

				try{
					SimpleDateFormat sdf4 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
					date4 = sdf4.parse(dateOutput2);
					System.out.println("Date 4 = " + date4);
				}catch(Exception e){
					System.err.println(e);
				}

				if(date4.compareTo(date3)>0){
					System.out.println("modified after the date requested");
					modified = true;
				}else{
					System.out.println("not modified after the date requested");
					modified = false;
				}
			}else{
				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream());
					outToClient.writeBytes("HTTP/1.0 400 Bad Request");
					System.out.println("400");   /*bad request*/
					outToClient.flush();
					t.sleep(500);
					try{
						inFromClient.close();		/*close the streams*/
						outToClient.close();
						HTTP1Server.decrease();
						return;
					}catch(Exception e){
						System.out.println(e);
						HTTP1Server.decrease();
						return;
					}

				}catch(Exception fe){
					System.out.println(fe);
				}

			}
		}else if(headerInfo.length()>17&& headerInfo.length() <34&&countHead==1){
				
				modified = true;
				System.out.println("were inside the 17-34");



		}else if(headerInfo.length()>1&&countHead==1){

				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream());
					outToClient.writeBytes("HTTP/1.0 400 Bad Request");
					System.out.println("400 inside the headerInfo>1");
					outToClient.flush();
					t.sleep(500);
					try{
						inFromClient.close();		/*close the streams*/
						outToClient.close();
						HTTP1Server.decrease();
						return;
					}catch(Exception e){
						System.out.println(e);
						HTTP1Server.decrease();
						return;
					}

				}catch(Exception fe){
					System.out.println(fe);
				}


		}else{
		}
		String byteStr = "";
		int byteS = 0;
		byte[] byteArr = new byte[(int)f.length()];	
		int countByte = 0;
		OutputStream out = null;

		if(head.compareTo(command)==0){      /*START THE OUTPUT STUFF HERE*/
			if(versionDub<=1.0){
				if(f.exists() && !f.isDirectory()){
					if(canReadFile == true){
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream()); /*good input with POST*/
								
								reader1 = new BufferedReader(new FileReader(resourceF));
								while((text1 = reader1.readLine())!=null){
									textInFile1 = text1;
								}
												
								outToClient.writeBytes("HTTP/1.0 200 OK" + "\r\n" + "Allow: GET, POST, HEAD " + "\r\n" + "Content-Encoding: identity" + "\r\n" + "Expires: " + dateOutput + "\r\n" + "Content-Type: " + extensionType + "\r\n" +"Content-Length: " + f.length() + "\r\n" + "Last-Modified: " + dateOutput2);
								System.out.println("200");
								outToClient.flush();
								t.sleep(500);
							}catch(Exception e){
								System.err.println(e);
							}
								}else{
						try{ /*if the file is forbidden*/
							outToClient = new DataOutputStream(connectionSocket.getOutputStream());
							outToClient.writeBytes("HTTP/1.0 403 Forbidden");
							System.out.println("403");
							outToClient.flush();
							t.sleep(500);
						}catch(Exception fe){
							System.out.println(fe);
						}

					}
				}else{
					try{ /*not found*/
						outToClient = new DataOutputStream(connectionSocket.getOutputStream());
						outToClient.writeBytes("HTTP/1.0 404 Not Found");
						System.out.println("404");
						outToClient.flush();
						t.sleep(500);
					}catch(Exception e){
						System.out.println(e);
					}
				}
			}else if( type.charAt(0)=='H'&&type.charAt(1)=='T'&&type.charAt(2)=='T'&&type.charAt(3)=='P'&& type.charAt(4)=='/'&&type.charAt(5)>1){
				System.out.println("VERSION IS TOO HIGH");
				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream()); /*verison is too high*/
					outToClient.writeBytes("HTTP/1.0 505 HTTP Version Not Supported");
					outToClient.flush();
					t.sleep(500);
				}catch(Exception e){
					System.err.println(e);
				}
			}else{
				System.out.println("Bad input");
				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream()); /*bad input*/
					outToClient.writeBytes("HTTP/1.0 400 Bad Request");
					outToClient.flush();
					t.sleep(500);
				}catch(Exception e){
					System.err.println(e);
				}
			}
		}else if(get.compareTo(command)==0){
			if(versionDub<=1.0){
				if(f.exists()&& !f.isDirectory()){
					if(canReadFile==true){
						if(modified == true){
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream()); /*good input with HEAD*/
								reader2 = new BufferedReader(new FileReader(resourceF));
									while((text2 = reader2.readLine())!=null){
										textInFile2 = textInFile2 + text2+ '\n';
									}
								textLength = textInFile2.length();
								try{
									InputStream targetStream = new FileInputStream(resourceF);
					
									while((byteS = targetStream.read()) != -1){
										byteStr = byteStr + (char)byteS;
									}
								}catch(Exception e){
									System.err.println(e);
								}	
								outToClient.writeBytes("HTTP/1.0 200 OK" + "\r\n" + "Allow: GET, POST, HEAD " + "\r\n" + "Content-Encoding: identity" + "\r\n" + "Expires: " + dateOutput + "\r\n" + "Content-Type: " + extensionType + "\r\n" +"Content-Length: " + f.length() + "\r\n" + "Last-Modified: " + dateOutput2 + "\r\n" + "\r\n" + byteStr);
														System.out.println("200");
								outToClient.flush();
								t.sleep(500);
							}catch(Exception e){
								System.err.println(e);
								error = 1;
							}
						}else{
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream());
								try{
									InputStream targetStream = new FileInputStream(resourceF);
									
									while((byteS = targetStream.read()) != -1){
										byteStr = byteStr + (char)byteS;
									}
								}catch(Exception e){
									System.err.println(e);
								}	
								outToClient.writeBytes("HTTP/1.0 304 Not Modified"+ "\r\n" + "Expires: "+ dateOutput+"\r\n"+"\r\n"+byteStr);
								System.out.println("304");
								outToClient.flush();
								t.sleep(500);
							}catch(Exception fe){
								System.out.println(fe);
							}
	


						}
					}else{
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream());
								outToClient.writeBytes("HTTP/1.0 403 Forbidden");
								System.out.println("403");
								outToClient.flush();
								t.sleep(500); /*forbidden*/
							}catch(Exception fe){
								System.out.println(fe);
							}
					
					}
				}else{
					try{
						outToClient = new DataOutputStream(connectionSocket.getOutputStream());
						outToClient.writeBytes("HTTP/1.0 404 Not Found");
						System.out.println("404");
						outToClient.flush();
						t.sleep(500);
					}catch(Exception e){
						System.out.println(e);
					}
				}
		
			}else if( type.charAt(0)=='H'&&type.charAt(1)=='T'&&type.charAt(2)=='T'&&type.charAt(3)=='P'&& type.charAt(4)=='/'&&type.charAt(5)>1){
				System.out.println("VERSION IS TOO HIGH");				/*version is too high*/
				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream());
					outToClient.writeBytes("HTTP/1.0 505 HTTP Version Not Supported");
					outToClient.flush();
					t.sleep(500);
				}catch(Exception e){
					System.err.println(e);
				}
			}else{
				System.out.println("Bad input");		/*bad input*/
				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream());
					outToClient.writeBytes("HTTP/1.0 400 Bad Request");
					outToClient.flush();
					t.sleep(500);
				}catch(Exception e){
					System.err.println(e);
				}
			}
			
		}else if(post.compareTo(command)==0){
			if(versionDub<=1){
				if(f.exists()&& !f.isDirectory()){
					if(canReadFile==true&&(canExecuteFile==true||extension.compareTo(".cgi")!=0)){ /*405 method not allowed*/
						if((extension.compareTo(".cgi")!=0)&&(contentLength!="")){
							System.out.println("http/1.0 405 method not allowed");
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream());
								outToClient.writeBytes("HTTP/1.0 405 Method Not Allowed");
								outToClient.flush();
								t.sleep(500);
							}catch(Exception e){
								System.err.println(e);
							}

						}else if(contentLength.compareTo("")==0||isLenGood==false){
							System.out.println("http/1.0 411 length required");
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream()); /*411 length required*/
								outToClient.writeBytes("HTTP/1.0 411 Length Required");
								outToClient.flush();
								t.sleep(500);
							}catch(Exception e){
								System.err.println(e);
							}

						}else if((contentType.compareTo("Content-Type: application/x-www-form-urlencoded")!=0)||(contentType.compareTo("")==0)){
								System.out.println("we are here");
								System.out.println("sCL is:");
								System.out.println("internal service error");
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream());
								outToClient.writeBytes("HTTP/1.0 500 Internal Server Error");  /*500 ISE*/
								outToClient.flush();
								t.sleep(500);
							}catch(Exception e){
								System.err.println(e);
							}
						}else if((contentType.compareTo("Content-Type: application/x-www-form-urlencoded")==0) && sCL!=""){
							String newPayload = "";
							newPayload=java.net.URLDecoder.decode(payload);
							String[] env = new String[6];
							String outPS ="";
							String tempS = "";
							String payOut = "";
							Process p = null;
							BufferedReader bri = null;
							OutputStreamWriter stdin = null;
							InputStreamReader stdout = null;
							
							if(contentLength.compareTo("")!=0){
								env[0] = "CONTENT_LENGTH="+newPayload.length();
							}else{
								env[0] = "CONTENT_LENGTH=";
							}
							env[1] = "SCRIPT_NAME="+resourceP;
							try{
								env[2] = "SERVER_NAME="+InetAddress.getLocalHost();
							}catch(Exception e){
								System.err.println(e);
								env[2] = "SERVER_NAME=";
							}
							env[3] = "SERVER_PORT="+port;
							if(from.compareTo("")!=0){
								env[4] = "HTTP_FROM="+from.substring(6,from.length());
							}else{
								env[4] = "HTTP_FROM=";
							}
							if(userAgent.compareTo("")!=0){
								env[5] = "HTTP_USER_AGENT=" + userAgent.substring(12,userAgent.length());
							}else{
								env[5] = "HTTP_USER_AGENT=";
							}
							for(int asdf = 0; asdf<6;asdf++){
								System.out.println("1. "+env[asdf]);
							}
							try{
								System.out.println("200 try 1");
								p = Runtime.getRuntime().exec(resourceF,env);
							
								System.out.println("200 try 2");
								stdin = new OutputStreamWriter(p.getOutputStream());
								System.out.println("200 try 3");
								stdin.write(newPayload);
									
								stdin.close();
								System.out.println("200 try 4");
								bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
								System.out.println("200 try 5");
								while((tempS= bri.readLine())!=null){
									System.out.println("inside the 200 try loop");
									payOut = payOut + tempS + '\n';
								}
								payOut = payOut.substring(0,payOut.length()-1);
								bri.close();
								System.out.println("200 try 6");
							}catch(Exception e){
								System.out.println("the 200 try is not working");
								System.err.print(e);
							}			
							System.out.println("payOut: " + payOut);	
							System.out.println("payOut size: " + payOut.length());
							if(payOut!=""){
								System.out.println("200");
								try{
									outToClient = new DataOutputStream(connectionSocket.getOutputStream());
								outToClient.writeBytes("HTTP/1.0 200 OK" + "\r\n" + "Allow: GET, POST, HEAD " + "\r\n" + "Content-Encoding: identity" + "\r\n" + "Expires: " + dateOutput + "\r\n" + "Content-Type: text/html" + "\r\n" +"Content-Length: " + payOut.length() + "\r\n" + "\r\n" + payOut);
									outToClient.flush();
									t.sleep(500);
								}catch(Exception e){
									System.err.println(e);
								}
							}else{
								System.out.println("204");
								try{
									outToClient = new DataOutputStream(connectionSocket.getOutputStream());
									outToClient.writeBytes("HTTP/1.0 204 No Content");
									outToClient.flush();
									t.sleep(500);
								}catch(Exception e){
									System.err.println(e);
								}

							}
						}else{
							System.out.println("HTTP/1.0 400 Bad Request in the everything is good part");
							try{
								outToClient = new DataOutputStream(connectionSocket.getOutputStream());
								outToClient.writeBytes("HTTP/1.0 400 Bad Request");
								outToClient.flush();
								t.sleep(500);
							}catch(Exception e){
								System.err.println(e);
							}

						}

					}else{
						try{
							outToClient = new DataOutputStream(connectionSocket.getOutputStream());
							outToClient.writeBytes("HTTP/1.0 403 Forbidden");
							System.out.println("403");
							outToClient.flush();
							t.sleep(500); /*forbidden*/
						}catch(Exception fe){
							System.out.println(fe);
						}
					}
				}else{
					System.out.println("HTTP/1.0 404 Not Found");
					try{
						outToClient = new DataOutputStream(connectionSocket.getOutputStream());
						outToClient.writeBytes("HTTP/1.0 404 Not Found");
						outToClient.flush();
						t.sleep(500);
					}catch(Exception e){
						System.err.println(e);
					}

				}
			}else{
	
				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream());
					outToClient.writeBytes("HTTP/1.0 505 HTTP Version Not Supported");
					outToClient.flush();
					t.sleep(500);
				}catch(Exception e){
					System.err.println(e);
				}

			}	
		}else if(((command.compareTo("PUT")==0)||(command.compareTo("DELETE")==0)||(command.compareTo("LINK")==0)||(command.compareTo("UNLINK")==0))){
			if(versionDub<=1.0){
				for(int q = 0; q<command.length();q++){
					if (command.charAt(q)>=65&&command.charAt(q)<=90){
					}else{
						error = 1;
						try{
							outToClient = new DataOutputStream(connectionSocket.getOutputStream());
							outToClient.writeBytes("HTTP/1.0 400 Bad Request");
							outToClient.flush();
							t.sleep(500); /*bad reuqest*/
						}catch(Exception e){
							System.err.println(e);
						}
						break;
					}
				}
				if(error ==0){
					try{
						outToClient = new DataOutputStream(connectionSocket.getOutputStream());
						outToClient.writeBytes("HTTP/1.0 501 Not Implemented");
						outToClient.flush(); /*not implemeneted*/
						t.sleep(500);
					}catch(Exception e){
						System.err.println(e);
					}
				}
			}else{
				System.out.println("VERSION IS TOO HIGH");				/*version is too high*/
				try{
					outToClient = new DataOutputStream(connectionSocket.getOutputStream());
					outToClient.writeBytes("HTTP/1.0 505 HTTP Version Not Supported");
					outToClient.flush();
					t.sleep(500);
				}catch(Exception e){
					System.err.println(e);
				}


			}

		}else{
			System.out.println("Bad input");	/*bad input*/
			try{
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				outToClient.writeBytes("HTTP/1.0 400 Bad Request");
				outToClient.flush();
				t.sleep(500);
			}catch(Exception e){
				System.err.println(e);
			}
		}

		try{
			inFromClient.close();		/*close the streams*/
			outToClient.close();
			HTTP1Server.decrease();
			return;
		}catch(Exception e){
			System.out.println(e);
			HTTP1Server.decrease();
			return;
		}
	}
	


	public void start(){
		if (t==null){
			t=new Thread(this,threadName);
			t.start();
		}
	}
}

import java.net.*;
import java.io.*;

public class FileServerHandler implements Runnable {
	// static final String rootPath =
	// "C:\\Users\\ALEXWELL\\workspace\\Exec1\\src\\Exec1\\root"; // rootĿ¼��ַ
	private Socket socket;
	private BufferedReader br;
	private BufferedWriter bw;
	private PrintWriter pw;
	private String rootPath;
	private String currentPath;
	private DatagramSocket datagramSocket;
	private VideoServer videoServer;

	/**
	 * Initialization
	 */
	public FileServerHandler(Socket socket, DatagramSocket datagramSocket, String rootPath) throws IOException {
		this.socket = socket;
		this.datagramSocket = datagramSocket;
		this.rootPath = rootPath;
		this.currentPath = rootPath;
	}

	/**
	 * Initialize Stream
	 */
	private void initStream() throws IOException {
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		pw = new PrintWriter(bw, true);
	}

	/**
	 * @param filePath
	 * 
	 * @return structure
	 * 
	 *         The filePath is the path direct to the target folder and this
	 *         function return the folder archive within a String structure
	 * 
	 */
	public static String readFile(String filePath) throws FileNotFoundException, IOException {// ʹ��%��Ϊÿһ�еķָ����������滻���з���ʹ��ÿһ�δ������ʹ��pw.printlnһ�η���
		String structure = "";// �ļ��ṹ��Ϣ

		File file = new File(filePath);// ���ļ�·��
		File[] filelist = file.listFiles();// ��ȡ��ǰ·���µ��ļ��ṹ
		for (int i = 0; i < filelist.length; i++) {
			if (filelist[i].isFile()) {
				structure = structure + "<file>" + " " + filelist[i].getName() + " " + filelist[i].length() + "%";// ��structure��д���ļ���Ϣ
			} else if (filelist[i].isDirectory()) {
				structure = structure + "<dir>" + " " + filelist[i].getName() + " " + filelist[i].length() + "%";// ��structure��д���ļ�����Ϣ
			}
		}
		return structure;
	}

	/**
	 * @param Path
	 *            The Path is current path where the user located
	 * 
	 * @param pw
	 *            The PrintWriter pw is used for print error info to user screen
	 * 
	 * @return newPath newPath is the Parent path of current path
	 * 
	 *         getParentFolder function returns the parent folder path to the server
	 *         If Path is root Path then return "1"
	 */
	public static String getParentFolder(String Path, PrintWriter pw) throws NullPointerException {
		String newPath = null;
		String father = null;
		String[] currentPath = null;
		String[] fatherPath = null;
		File file = new File(Path);// ���ļ�·��
		currentPath = Path.split("\\\\");
		if (currentPath[currentPath.length - 1].equals("root")) {
			// ��ǰ·��Ϊroot
			pw.println("now is in root dir");
			return "1";
		} else {
			// ��ǰ·����Ϊroot
			newPath = file.getParent();
			fatherPath = newPath.split("\\\\");
			father = fatherPath[fatherPath.length - 1] + " > OK";
			pw.println(father);
			return newPath;
		}
	}

	
	
	
	/**
	 * @param filePath
	 *            The filePath is the path where the file located
	 * 
	 *            getParentFolder function return the parent folder path to the
	 *            server
	 */
	public void sendFile(String filePath) throws IOException, InterruptedException {
		// init
		File file = new File(filePath);
		// datagramSocket = new DatagramSocket(FileServer.UDP_PORT);
		DatagramPacket datagramPacket;

		// get port
		datagramPacket = new DatagramPacket(new byte[1024], 1024);// init a dummy dp to receive a packet from client to
																	// get port
		datagramSocket.receive(datagramPacket);// ����һ����������շ���������udp���ݣ��õ�������udp���ж˿�
		int clientPort = datagramPacket.getPort();
		BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));

		// send file
		int read = -1;
		while (true) {
			byte[] sendDataByte = new byte[1024];
			if (bufferedInputStream != null) {
				read = bufferedInputStream.read(sendDataByte);
			}
			if (read == -1) {
				break;
			}
			datagramPacket = new DatagramPacket(sendDataByte, sendDataByte.length, socket.getInetAddress(), clientPort);
			datagramSocket.send(datagramPacket);
			Thread.sleep(1);
		}
		bufferedInputStream.close();// release source
		byte[] sendEndMessage = "end".getBytes();
		datagramPacket = new DatagramPacket(sendEndMessage, sendEndMessage.length, socket.getInetAddress(), clientPort);
		datagramSocket.send(datagramPacket);
		

	}

	// Override the thread run method
	@Override
	public void run() {
		currentPath = rootPath;
		try {
			initStream();
			// ��ǰ·������ʼ·��Ϊroot

			// ��ǰ·���µ�Ŀ¼�ṹ
			String pwdFileStructure = null;
			// �������ݷ��ͻ�����

			InetAddress byName = InetAddress.getByName("127.0.0.1");
			System.out.println("�����ӣ����ӵ�ַ��" + socket.getInetAddress() + "��" + socket.getPort()); // �ͻ�����Ϣ
			// ����������ȡ�ͻ�����Ϣ
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// ���������ͻ���д��Ϣ
			bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

			pw = new PrintWriter(bw, true); // װ���������true,ÿдһ�о�ˢ�����������������flush
			// ���浱ǰ·�����ļ��ṹ
			try {
				pwdFileStructure = readFile(currentPath);

			} catch (FileNotFoundException e) {
				pw.println("Invalid directory");
			}

			// �ȴ���ȡ���û����ӣ��������׽���

			String info = null; // �����û��������Ϣ
			String[] command = null;
			while ((info = br.readLine()) != null) {
				command = info.split("\\s+");// ʹ�ÿո�ָ�
				// ls command
				if (command[0].equals("ls")) { // ����û����롰ls���ͷ���Ŀ¼�ṹ
					try {
						pwdFileStructure = readFile(currentPath);

					} catch (FileNotFoundException e) {
						pw.println("Invalid directory");
					}
					pw.println(pwdFileStructure); // ��ͻ��˷���rootĿ¼�ṹ��Ϣ
					
				}
				// cd command
				else if (command[0].equals("cd")) {
					if (!command[1].equals("..")) {
						// �û�����һ��Ŀ¼
						try {
							pwdFileStructure = readFile(currentPath + "/" + command[1]);
							currentPath = currentPath + "/" + command[1];
							pw.println(command[1] + " > OK");
						} catch (Exception e) {
							pw.println("Directory Notfound");
						}
					} else if (command[1].equals("..")) {// �û�����".."������һ���˵���rootĿ¼������
						String tempPath = null;
						tempPath = getParentFolder(currentPath, pw);
						// ���tempPath��1����ô֤��currentPath��rootPath������Ҫ�ı�currentPath
						if (!tempPath.equals("1")) {
							currentPath = tempPath;
							try {
								pwdFileStructure = readFile(currentPath);
							} catch (Exception e) {
								pw.println("ED");
							}
						}
					} else {
						// cd��û�в��������ز���������Ϣ
						pw.println("parameter error");
					}
				}
				// get command
				else if (command[0].equals("get")) {
					String filePath = this.currentPath + "\\" + command[1];
					File file = new File(filePath);
					if (!file.exists()) {
						pw.println("-1");// file not exist
						continue;
					}
					if (!file.isFile()) {
						pw.println("-2");// path is a directory
						continue;
					} else {
						pw.println("0");
					}
					sendFile(filePath);
					// ��������
				} else if (command[0].equals("bye")) { // ����û����롰bye�����˳�
					break;
				} else if(command[0].equals("del")) {//����û�����del��ɾ���ļ�
					String filePath = this.currentPath + "\\" + command[1];
					if(DeleteFileUtil.delete(filePath)) {
						pw.println("0");//����0��ʾɾ���ɹ�
					}else {
						pw.println("-1");//����-1��ʾɾ��ʧ��
					}
				} else if(command[0].equals("screen")) {
					CaptureScreen.captureScreen(rootPath, "screen.png");
					String filePath = rootPath+"\\screen.png";
					sendFile(filePath);
				} else if(command[0].equals("send")) {
					/*
					 * Client��Server�����ļ�ʹ��20200�˿�
					 */
					DatagramSocket datagramSocket = new DatagramSocket();// ���͵����ݰ�
					byte[] fileBuffer = new byte[1024];// ���ݻ�����
					String sendString = "Get Command";// ���͵�udp����
					fileBuffer = sendString.getBytes();// ������д�����ݻ�������
					DatagramPacket datagramPacket = new DatagramPacket(fileBuffer, fileBuffer.length,
							InetAddress.getByName("localhost"), 20200);// �����ݻ������е�����д��datagramPacket���õ�����ipʹ��udp�˿ڽ������ݰ�
					datagramSocket.send(datagramPacket);// datagramPacket���������������
					// ׼�������ļ�
					String fileName = rootPath + "\\" + command[1];// ��Ŀ���ļ���׼����
					File file = new File(fileName);// �ڵ�ǰ�ͻ��θ�Ŀ¼�½�һ��Ŀ���ļ�����
					FileOutputStream fileOutputStream = new FileOutputStream(file);// ׼���ļ������
					System.out.println("��ʼ�����ļ�");// ��ʼ�����ļ�
					while (true) {
						byte[] receiveDataByte = new byte[1024];
						datagramPacket = new DatagramPacket(receiveDataByte, receiveDataByte.length);
						datagramSocket.receive(datagramPacket);
						byte[] data = datagramPacket.getData();
						if (new String(data).startsWith("end")) {
							System.out.println("�ļ��������");
							fileOutputStream.close();
							datagramSocket.close();
							break;
						} else {
							fileOutputStream.write(data);
						}
					}
				} else if(command[0].equals("video")) {
					VideoServer videoServer = new VideoServer();
					videoServer.start();
				} else if(command[0].equals("mkdir")) {
					String fileName = rootPath + "\\" + command[1];// ��Ŀ���ļ���׼����
					File file = new File(fileName);
					if(file.exists()) {
						pw.println("-2");//����-2��ʾ�ļ�����
					}else if(file.mkdir()){
						pw.println("0");//����0��ʾ�ɹ�
					}else {
						pw.println("-1");//����-1��ʾʧ��
					}
				}
				else {
					pw.println("Unknow Command!");
				}
			}
		} // ����ͻ��˶Ͽ����ӣ���Ӧ������쳣������Ӧ�ж�����whileѭ����ʹ�÷������ܼ����������ͻ���ͨ��
		catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != socket) {
				try {
					datagramSocket.close();// release source
					socket.close(); // �Ͽ�����
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}

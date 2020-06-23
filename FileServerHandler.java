import java.net.*;
import java.io.*;

public class FileServerHandler implements Runnable {
	// static final String rootPath =
	// "C:\\Users\\ALEXWELL\\workspace\\Exec1\\src\\Exec1\\root"; // root目录地址
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
	public static String readFile(String filePath) throws FileNotFoundException, IOException {// 使用%作为每一行的分隔符，用来替换换行符，使得每一次传输可以使用pw.println一次发送
		String structure = "";// 文件结构信息

		File file = new File(filePath);// 打开文件路径
		File[] filelist = file.listFiles();// 获取当前路径下的文件结构
		for (int i = 0; i < filelist.length; i++) {
			if (filelist[i].isFile()) {
				structure = structure + "<file>" + " " + filelist[i].getName() + " " + filelist[i].length() + "%";// 向structure中写入文件信息
			} else if (filelist[i].isDirectory()) {
				structure = structure + "<dir>" + " " + filelist[i].getName() + " " + filelist[i].length() + "%";// 向structure中写入文件夹信息
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
		File file = new File(Path);// 打开文件路径
		currentPath = Path.split("\\\\");
		if (currentPath[currentPath.length - 1].equals("root")) {
			// 当前路径为root
			pw.println("now is in root dir");
			return "1";
		} else {
			// 当前路径不为root
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
		datagramSocket.receive(datagramPacket);// 创建一个虚拟包接收服务器发送udp数据，得到服务器udp运行端口
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
			// 当前路径，初始路径为root

			// 当前路径下的目录结构
			String pwdFileStructure = null;
			// 创建数据发送缓冲区

			InetAddress byName = InetAddress.getByName("127.0.0.1");
			System.out.println("新连接，连接地址：" + socket.getInetAddress() + "：" + socket.getPort()); // 客户端信息
			// 输入流，读取客户端信息
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// 输出流，向客户端写信息
			bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

			pw = new PrintWriter(bw, true); // 装饰输出流，true,每写一行就刷新输出缓冲区，不用flush
			// 保存当前路径的文件结构
			try {
				pwdFileStructure = readFile(currentPath);

			} catch (FileNotFoundException e) {
				pw.println("Invalid directory");
			}

			// 等待并取出用户连接，并创建套接字

			String info = null; // 接收用户输入的信息
			String[] command = null;
			while ((info = br.readLine()) != null) {
				command = info.split("\\s+");// 使用空格分隔
				// ls command
				if (command[0].equals("ls")) { // 如果用户输入“ls”就返回目录结构
					try {
						pwdFileStructure = readFile(currentPath);

					} catch (FileNotFoundException e) {
						pw.println("Invalid directory");
					}
					pw.println(pwdFileStructure); // 向客户端返回root目录结构信息
					
				}
				// cd command
				else if (command[0].equals("cd")) {
					if (!command[1].equals("..")) {
						// 用户输入一个目录
						try {
							pwdFileStructure = readFile(currentPath + "/" + command[1]);
							currentPath = currentPath + "/" + command[1];
							pw.println(command[1] + " > OK");
						} catch (Exception e) {
							pw.println("Directory Notfound");
						}
					} else if (command[1].equals("..")) {// 用户输入".."返回上一级菜单，root目录不返回
						String tempPath = null;
						tempPath = getParentFolder(currentPath, pw);
						// 如果tempPath是1，那么证明currentPath是rootPath，不需要改变currentPath
						if (!tempPath.equals("1")) {
							currentPath = tempPath;
							try {
								pwdFileStructure = readFile(currentPath);
							} catch (Exception e) {
								pw.println("ED");
							}
						}
					} else {
						// cd后没有参数，返回参数错误信息
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
					// 结束连接
				} else if (command[0].equals("bye")) { // 如果用户输入“bye”就退出
					break;
				} else if(command[0].equals("del")) {//如果用户输入del就删除文件
					String filePath = this.currentPath + "\\" + command[1];
					if(DeleteFileUtil.delete(filePath)) {
						pw.println("0");//返回0表示删除成功
					}else {
						pw.println("-1");//返回-1表示删除失败
					}
				} else if(command[0].equals("screen")) {
					CaptureScreen.captureScreen(rootPath, "screen.png");
					String filePath = rootPath+"\\screen.png";
					sendFile(filePath);
				} else if(command[0].equals("send")) {
					/*
					 * Client向Server发送文件使用20200端口
					 */
					DatagramSocket datagramSocket = new DatagramSocket();// 发送的数据包
					byte[] fileBuffer = new byte[1024];// 数据缓冲区
					String sendString = "Get Command";// 发送的udp数据
					fileBuffer = sendString.getBytes();// 将数据写入数据缓冲区中
					DatagramPacket datagramPacket = new DatagramPacket(fileBuffer, fileBuffer.length,
							InetAddress.getByName("localhost"), 20200);// 将数据缓冲区中的数据写入datagramPacket，得到主机ip使用udp端口建立数据包
					datagramSocket.send(datagramPacket);// datagramPacket向服务器发送数据
					// 准备接收文件
					String fileName = rootPath + "\\" + command[1];// 将目标文件名准备好
					File file = new File(fileName);// 在当前客户段根目录新建一个目标文件并打开
					FileOutputStream fileOutputStream = new FileOutputStream(file);// 准备文件输出流
					System.out.println("开始接收文件");// 开始接收文件
					while (true) {
						byte[] receiveDataByte = new byte[1024];
						datagramPacket = new DatagramPacket(receiveDataByte, receiveDataByte.length);
						datagramSocket.receive(datagramPacket);
						byte[] data = datagramPacket.getData();
						if (new String(data).startsWith("end")) {
							System.out.println("文件接收完毕");
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
					String fileName = rootPath + "\\" + command[1];// 将目标文件名准备好
					File file = new File(fileName);
					if(file.exists()) {
						pw.println("-2");//返回-2表示文件存在
					}else if(file.mkdir()){
						pw.println("0");//返回0表示成功
					}else {
						pw.println("-1");//返回-1表示失败
					}
				}
				else {
					pw.println("Unknow Command!");
				}
			}
		} // 如果客户端断开连接，则应捕获该异常，但不应中断整个while循环，使得服务器能继续与其他客户端通信
		catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != socket) {
				try {
					datagramSocket.close();// release source
					socket.close(); // 断开连接
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}

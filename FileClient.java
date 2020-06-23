import java.io.*;
import java.net.*;
import java.util.Scanner;

import com.sun.media.jfxmedia.control.VideoDataBuffer;


public class FileClient {
	static final int TCP_PORT = 2021; // 连接端口
	static final int UDP_PORT = 20200;
	static final String HOST = "127.0.0.1"; // 连接地址
	private Socket socket;
	private BufferedReader br;
	private BufferedWriter bw;
	private PrintWriter pw;
	private DatagramSocket datagramSocket;
	private VideoClient videoClient;
	
	
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
	
	/**
	 * Initialize Client
	 * 
	 */
	public FileClient() throws UnknownHostException, IOException {
		socket = new Socket(HOST, TCP_PORT);
		datagramSocket = new DatagramSocket(UDP_PORT);
	}

	/**
	 * set the client root path and run the client
	 * @throws InterruptedException 
	 * 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		String clientRoot = args[0];
		FileClient fileClient = new FileClient();
		fileClient.run(clientRoot);
	}

	/**
	 * Initialize the I/O stream
	 * 
	 */
	private void initStream() throws IOException {
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		pw = new PrintWriter(bw, true);
	}

	/**
	 * @param path
	 *            A string which shows the archive of target folder
	 * 
	 *            This function help the client decode the archive of file folder
	 *            which "\n" Replace with "%"
	 */
	public static void structureDecode(String path) {// 使用%解码文件结构
		String[] filePath = path.split("%");
		for (int i = 0; i < filePath.length; i++) {
			System.out.println(filePath[i]);
		}
	}	

	/**
	 * run implements
	 * @throws InterruptedException 
	 */
	public void run(String rootPath) throws InterruptedException {
		try {
			initStream();
			// 客户端输出流，向服务器发消息
			Scanner in = new Scanner(System.in); // 接受用户信息
			String msg = null;
			String fileStructure = null;
			while ((msg = in.nextLine()) != null) {
				if (msg.startsWith("ls")) {
					pw.println(msg); // 发送给服务器端
					fileStructure = br.readLine();
					structureDecode(fileStructure);// 解码接收到的文件结构
					continue;
				} else if (msg.startsWith("cd")) {
					String[] command = msg.split("\\s+");// 空格分隔命令
					if (command.length < 2) {// 判断命令是否符合要求
						System.out.println("Error Parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// 判断路径是否正确
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // 发送给服务器端
					String result = br.readLine();// 得到服务器返回的信息
					// System.out.println(br.readLine()); //输出服务器返回的消息
					if (result.equals("ED")) {
						System.out.println("Invalid directory");// 无效路径
					} else {
						System.out.println(result);// 打印成功信息
					}
				} else if (msg.startsWith("get")) {
					// UDP接收文件
					// 发送get命令
					String[] command = msg.split("\\s+");// 空格分隔命令
					if (command.length < 2) {// 判断命令是否符合要求
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// 判断路径是否正确
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // 发送给服务器端
					String result = br.readLine();
					if (result.equals("-1")) {
						System.out.println("File not exists");// server return -1 means that file not exist
						continue;
					}
					if (result.equals("-2")) {
						System.out.println("Its a directory");// server return -2 means that the path is a directory
						continue;
					}
					// System.out.println(result);
					// 准备发送udp数据
					DatagramSocket datagramSocket = new DatagramSocket();// 发送的数据包
					byte[] fileBuffer = new byte[1024];// 数据缓冲区
					String sendString = "Get Command";// 发送的udp数据
					fileBuffer = sendString.getBytes();// 将数据写入数据缓冲区中
					DatagramPacket datagramPacket = new DatagramPacket(fileBuffer, fileBuffer.length,
							InetAddress.getByName(HOST),2020);// 将数据缓冲区中的数据写入datagramPacket，得到主机ip使用udp端口建立数据包
					datagramSocket.send(datagramPacket);// datagramPacket向服务器发送数据
					// 准备接收文件
					String fileName = rootPath + "\\" + command[1];// 将目标文件名准备好
					File file = new File(fileName);// 在当前客户段根目录新建一个目标文件并打开
					FileOutputStream fileOutputStream = new FileOutputStream(file);// 准备文件输出流
					System.out.println("开始接收文件" + " " + command[1]);// 开始接收文件
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

				} else if (msg.equals("bye")) {
					break;
				} else if(msg.startsWith("del")) {
					String[] command = msg.split("\\s+");// 空格分隔命令
					if (command.length != 2) {// 判断命令是否符合要求
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// 判断路径是否正确
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // 发送给服务器端
					String result = br.readLine();
					if (result.equals("0")) {
						System.out.println("delete success");// server return 0 means that delete success
						continue;
					}
					if (result.equals("-1")) {
						System.out.println("delete fail");// server return -1 means that delete fail
						continue;
					}
				} else if(msg.startsWith("screen")) {
					/*
					 * Server向Client发送文件使用2020端口
					 */
					pw.println(msg);
//					System.out.println("Use get command 'get screen.png' to get image");
					DatagramSocket datagramSocket = new DatagramSocket();// 发送的数据包
					byte[] fileBuffer = new byte[1024];// 数据缓冲区
					String sendString = "Get Command";// 发送的udp数据
					fileBuffer = sendString.getBytes();// 将数据写入数据缓冲区中
					DatagramPacket datagramPacket = new DatagramPacket(fileBuffer, fileBuffer.length,
							InetAddress.getByName(HOST), 2020);// 将数据缓冲区中的数据写入datagramPacket，得到主机ip使用udp端口建立数据包
					datagramSocket.send(datagramPacket);// datagramPacket向服务器发送数据
					// 准备接收文件
					String fileName = rootPath + "\\" + "screen.png";// 将目标文件名准备好
					File file = new File(fileName);// 在当前客户段根目录新建一个目标文件并打开
					FileOutputStream fileOutputStream = new FileOutputStream(file);// 准备文件输出流
					System.out.println("开始接收文件" + " " + "screen.png");// 开始接收文件
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

				} else if(msg.startsWith("send")) {
					/*
					 * Client向Server发送文件使用20200端口
					 */
					String[] command = msg.split("\\s+");// 空格分隔命令
					if (command.length < 2) {// 判断命令是否符合要求
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// 判断路径是否正确
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // 发送给服务器端
					String filePath = rootPath + "\\" +command[1];
					sendFile(filePath);
				} else if(msg.startsWith("video")){
					pw.println(msg);
					VideoClient videoClient = new VideoClient();
					
				} else if(msg.startsWith("stop")) {
					videoClient.stopVideo();
					pw.println(msg);
					
				} else if(msg.startsWith("mkdir")) {
					String[] command = msg.split("\\s+");// 空格分隔命令
					if (command.length < 2) {// 判断命令是否符合要求
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// 判断路径是否正确
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg);
					String result = br.readLine();
					if (result.equals("0")) {
						System.out.println("create success");// server return 0 means that delete success
						continue;
					}
					if (result.equals("-1")) {
						System.out.println("create fail");// server return -1 means that delete fail
						continue;
					}
					if (result.equals("-2")) {
						System.out.println("dir exist");// server return -1 means that delete fail
						continue;
					}
				}
				else {
					System.out.println("Unknow command");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != socket) {
				try {
					socket.close(); // 断开连接
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

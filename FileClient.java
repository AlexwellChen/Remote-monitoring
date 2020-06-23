import java.io.*;
import java.net.*;
import java.util.Scanner;

import com.sun.media.jfxmedia.control.VideoDataBuffer;


public class FileClient {
	static final int TCP_PORT = 2021; // ���Ӷ˿�
	static final int UDP_PORT = 20200;
	static final String HOST = "127.0.0.1"; // ���ӵ�ַ
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
	public static void structureDecode(String path) {// ʹ��%�����ļ��ṹ
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
			// �ͻ���������������������Ϣ
			Scanner in = new Scanner(System.in); // �����û���Ϣ
			String msg = null;
			String fileStructure = null;
			while ((msg = in.nextLine()) != null) {
				if (msg.startsWith("ls")) {
					pw.println(msg); // ���͸���������
					fileStructure = br.readLine();
					structureDecode(fileStructure);// ������յ����ļ��ṹ
					continue;
				} else if (msg.startsWith("cd")) {
					String[] command = msg.split("\\s+");// �ո�ָ�����
					if (command.length < 2) {// �ж������Ƿ����Ҫ��
						System.out.println("Error Parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// �ж�·���Ƿ���ȷ
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // ���͸���������
					String result = br.readLine();// �õ����������ص���Ϣ
					// System.out.println(br.readLine()); //������������ص���Ϣ
					if (result.equals("ED")) {
						System.out.println("Invalid directory");// ��Ч·��
					} else {
						System.out.println(result);// ��ӡ�ɹ���Ϣ
					}
				} else if (msg.startsWith("get")) {
					// UDP�����ļ�
					// ����get����
					String[] command = msg.split("\\s+");// �ո�ָ�����
					if (command.length < 2) {// �ж������Ƿ����Ҫ��
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// �ж�·���Ƿ���ȷ
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // ���͸���������
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
					// ׼������udp����
					DatagramSocket datagramSocket = new DatagramSocket();// ���͵����ݰ�
					byte[] fileBuffer = new byte[1024];// ���ݻ�����
					String sendString = "Get Command";// ���͵�udp����
					fileBuffer = sendString.getBytes();// ������д�����ݻ�������
					DatagramPacket datagramPacket = new DatagramPacket(fileBuffer, fileBuffer.length,
							InetAddress.getByName(HOST),2020);// �����ݻ������е�����д��datagramPacket���õ�����ipʹ��udp�˿ڽ������ݰ�
					datagramSocket.send(datagramPacket);// datagramPacket���������������
					// ׼�������ļ�
					String fileName = rootPath + "\\" + command[1];// ��Ŀ���ļ���׼����
					File file = new File(fileName);// �ڵ�ǰ�ͻ��θ�Ŀ¼�½�һ��Ŀ���ļ�����
					FileOutputStream fileOutputStream = new FileOutputStream(file);// ׼���ļ������
					System.out.println("��ʼ�����ļ�" + " " + command[1]);// ��ʼ�����ļ�
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

				} else if (msg.equals("bye")) {
					break;
				} else if(msg.startsWith("del")) {
					String[] command = msg.split("\\s+");// �ո�ָ�����
					if (command.length != 2) {// �ж������Ƿ����Ҫ��
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// �ж�·���Ƿ���ȷ
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // ���͸���������
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
					 * Server��Client�����ļ�ʹ��2020�˿�
					 */
					pw.println(msg);
//					System.out.println("Use get command 'get screen.png' to get image");
					DatagramSocket datagramSocket = new DatagramSocket();// ���͵����ݰ�
					byte[] fileBuffer = new byte[1024];// ���ݻ�����
					String sendString = "Get Command";// ���͵�udp����
					fileBuffer = sendString.getBytes();// ������д�����ݻ�������
					DatagramPacket datagramPacket = new DatagramPacket(fileBuffer, fileBuffer.length,
							InetAddress.getByName(HOST), 2020);// �����ݻ������е�����д��datagramPacket���õ�����ipʹ��udp�˿ڽ������ݰ�
					datagramSocket.send(datagramPacket);// datagramPacket���������������
					// ׼�������ļ�
					String fileName = rootPath + "\\" + "screen.png";// ��Ŀ���ļ���׼����
					File file = new File(fileName);// �ڵ�ǰ�ͻ��θ�Ŀ¼�½�һ��Ŀ���ļ�����
					FileOutputStream fileOutputStream = new FileOutputStream(file);// ׼���ļ������
					System.out.println("��ʼ�����ļ�" + " " + "screen.png");// ��ʼ�����ļ�
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

				} else if(msg.startsWith("send")) {
					/*
					 * Client��Server�����ļ�ʹ��20200�˿�
					 */
					String[] command = msg.split("\\s+");// �ո�ָ�����
					if (command.length < 2) {// �ж������Ƿ����Ҫ��
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// �ж�·���Ƿ���ȷ
						System.out.println("Error directory");
						continue;
					}
					pw.println(msg); // ���͸���������
					String filePath = rootPath + "\\" +command[1];
					sendFile(filePath);
				} else if(msg.startsWith("video")){
					pw.println(msg);
					VideoClient videoClient = new VideoClient();
					
				} else if(msg.startsWith("stop")) {
					videoClient.stopVideo();
					pw.println(msg);
					
				} else if(msg.startsWith("mkdir")) {
					String[] command = msg.split("\\s+");// �ո�ָ�����
					if (command.length < 2) {// �ж������Ƿ����Ҫ��
						System.out.println("Error parameter");
						continue;
					}
					if (command[1].startsWith("\\")) {// �ж�·���Ƿ���ȷ
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
					socket.close(); // �Ͽ�����
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

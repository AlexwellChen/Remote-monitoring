import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {
	static final int TCP_PORT = 2021;
	static final int UDP_PORT = 2020;
	private final int POOL_SIZE = 4;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private ExecutorService executorService;

	/**
	 * ��ʼ��
	 */
	private FileServer() throws IOException {
		this.serverSocket = new ServerSocket(TCP_PORT, 2);
		this.datagramSocket = new DatagramSocket(UDP_PORT);
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);
		System.out.println("������������");

	}

	/**
	 * @param args
	 * 
	 * @throws IOException
	 * 
	 */
	public static void main(String[] args) throws IOException {

		// ���õ�·������ȷ���ش�����Ϣ
		if (args.length != 1) {// �ж����������
			System.out.println("Invalid parameters!");
			return;
		}
		File file = new File(args[0]);// ����root·��
		if (file.exists()) {
			if (file.isDirectory()) {
				// ���root·���Ƿ����ļ���
				new FileServer().service(args[0]);
			} else {
				// ·�������ļ��У����ش�����Ϣ
				System.out.println("Invalid directory");
			}
		} else {
			// ·��������
			System.out.println("Path not exist");
		}

	}

	/**
	 * ��������
	 * 
	 * @param path
	 *            �����root·��
	 * 
	 */
	private void service(String path) {
		Socket socket = null;
		while (true) {
			try {
				socket = serverSocket.accept();
				executorService.execute(new FileServerHandler(socket, datagramSocket, path));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}

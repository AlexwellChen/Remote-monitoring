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
	 * 初始化
	 */
	private FileServer() throws IOException {
		this.serverSocket = new ServerSocket(TCP_PORT, 2);
		this.datagramSocket = new DatagramSocket(UDP_PORT);
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);
		System.out.println("服务器启动。");

	}

	/**
	 * @param args
	 * 
	 * @throws IOException
	 * 
	 */
	public static void main(String[] args) throws IOException {

		// 设置的路径不正确返回错误信息
		if (args.length != 1) {// 有多个参数传入
			System.out.println("Invalid parameters!");
			return;
		}
		File file = new File(args[0]);// 读入root路径
		if (file.exists()) {
			if (file.isDirectory()) {
				// 检查root路径是否是文件夹
				new FileServer().service(args[0]);
			} else {
				// 路径不是文件夹，返回错误信息
				System.out.println("Invalid directory");
			}
		} else {
			// 路径不存在
			System.out.println("Path not exist");
		}

	}

	/**
	 * 启动服务
	 * 
	 * @param path
	 *            传入的root路径
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

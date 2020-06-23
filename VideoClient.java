import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.*;


public class VideoClient extends JFrame{
	private static final long serialVersionUID = 1L;
	Dimension screenSize;
	ServerSocket ss = null;
	Graphics2D g2;
	Image cimage;
	
	public VideoClient() {
		super();
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(800, 640);
		Screen p = new Screen();
		Container c = this.getContentPane();
		c.setLayout(new BorderLayout());
		c.add(p, SwingConstants.CENTER);
		new Thread(p).start();
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				setVisible(true);
			}});
	}
 
 
	class Screen extends JPanel implements Runnable {
 
		private static final long serialVersionUID = 1L;
		
 
		public void run() {
			try {
				ss = new ServerSocket(5001);// 探听5001端口的连接
				while (true) {
					Socket s = null;
					try {
						s = ss.accept();
						ZipInputStream zis = new ZipInputStream(s
								.getInputStream());
						zis.getNextEntry();
						cimage = ImageIO.read(zis);// 把ZIP流转换为图片
						repaint();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (s != null) {
							try {
								s.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} catch (Exception e) {
			} finally {
				if (ss != null) {
					try {
						ss.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		public Screen() {
			super();
			this.setLayout(null);
		}
 
		public void paint(Graphics g) {
			super.paint(g);
			g2 = (Graphics2D) g;
			g2.drawImage(cimage, 0, 0, null);
		}
		
	}

	
	public void stopVideo() throws IOException, InterruptedException {
		g2.dispose();
	}

}

package jp.etrobo.ev3.pcsample;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class RemoteClient extends Frame implements KeyListener {
	private static final long serialVersionUID = 1630664954341554884L;

	public static final int PORT = 7360;

	public static final int CLOSE = 0;
	public static final int START = 71; // 'g'
	public static final int STOP = 83; // 's'

	public static String LOG_FILE_PATH = "C:\\Users\\1090427\\ev3log\\";
	public String file_name = "";

	public File file;
	public FileWriter filewriter;
	public BufferedWriter bw;
	public PrintWriter pw;
	Button btnLog;
	Timer rcTimer = null;
	TimerTask rcTask = null;

	Button btnConnect;
	TextField txtIPAddress;
	TextArea messages;

	private Socket socket;
	private DataOutputStream outStream;

	private Socket client = null;
	private InputStream inputStream = null;
	private DataInputStream dataInputStream = null;

	public RemoteClient(String title, String ip) {
		super(title);
		this.setSize(400, 300);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Ending Warbird Client");
				disconnect();
				System.exit(0);
			}
		});
		buildGUI(ip);
		this.setVisible(true);
		btnConnect.addKeyListener(this);
		btnLog.addKeyListener(this);
	}

	public static void main(String args[]) {
		String ip = "10.0.1.1";
		if (args.length > 0)
			ip = args[0];
		System.out.println("Starting Client...");
		new RemoteClient("LeJOS EV3 Client Sample", ip);

	}

	public void buildGUI(String ip) {
		Panel mainPanel = new Panel(new BorderLayout());
		ControlListener cl = new ControlListener();
		btnConnect = new Button("Connect");
		btnConnect.addActionListener(cl);
		txtIPAddress = new TextField(ip, 16);
		btnLog = new Button("StartLog");
		btnLog.addActionListener(cl);
		messages = new TextArea("status: DISCONNECTED");
		messages.setEditable(false);
		Panel north = new Panel(new FlowLayout(FlowLayout.LEFT));
		north.add(btnConnect);
		north.add(txtIPAddress);
		north.add(btnLog);
		Panel center = new Panel(new GridLayout(5, 1));
		center.add(new Label("G to start, S to stop"));
		Panel center4 = new Panel(new FlowLayout(FlowLayout.LEFT));
		center4.add(messages);
		center.add(center4);
		mainPanel.add(north, "North");
		mainPanel.add(center, "Center");
		this.add(mainPanel);
	}

	private void sendCommand(int command) {
		messages.setText("status: SENDING command.");
		try {
			outStream.writeInt(command);
			messages.setText("status: Comand SENT");
		} catch (IOException io) {
			messages.setText("status: ERROR Probrems occurred sending data.");
		}
	}

	private class ControlListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();
			if (command.equals("Connect")) {
				try {
					socket = new Socket(txtIPAddress.getText(), PORT);
					outStream = new DataOutputStream(socket.getOutputStream());

					// server = new ServerSocket(PORT);
					// client = server.accept();
					client = socket;
					inputStream = client.getInputStream();
					dataInputStream = new DataInputStream(inputStream);

					messages.setText("status: CONNECTED");
					btnConnect.setLabel("Disconnect");

				} catch (Exception exc) {
					messages.setText("status: FAILURE Error establishing connection with EV3.");
					System.out.println("Error" + exc);
				}
			} else if (command.equals("Disconnect")) {
				disconnect();
			} else if (command.equals("StartLog")) {
				startLog();
			} else if (command.equals("EndLog")) {
				endLog();
			}
		}
	}

	public void disconnect() {
		try {
			sendCommand(CLOSE);
			socket.close();
			btnConnect.setLabel("Connect");
			messages.setText("status: DISCONNECTED");
			rcTimer.cancel();
			pw.close();
		} catch (Exception exc) {
			messages.setText("status: FAILURE Error closing connection with EV3.");
			System.out.println("Error" + exc);
		}
	}

	public void startLog() {
		// logファイル名生成
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String logStartYmd = sdf.format(c.getTime());
		file_name = LOG_FILE_PATH + logStartYmd + "_ev3log.txt";

		// logファイル書き込み準備
		try {
			file = new File(file_name);
			filewriter = new FileWriter(file);
			bw = new BufferedWriter(filewriter);
			pw = new PrintWriter(bw);

			System.out.println("ログ出力開始：" + file_name);
		} catch (IOException exc) {
		}

		// log書き込みタスク開始
		rcTimer = new Timer();
		rcTask = new TimerTask() { // リモートコマンドタスク
			@Override
			public void run() {
				if (client != null) {
					try {
						if (dataInputStream.available() > 0) {
							float a = dataInputStream.readFloat();
							pw.println(sdf2.format(new Date()) + " => " + a);
						}
					} catch (Exception exc) {
					}
				}
			}
		};
		rcTimer.schedule(rcTask, 0, 20);
		btnLog.setLabel("EndLog");
	}

	public void endLog() {
		rcTimer.cancel();
		pw.close();
		file_name = "";
		btnLog.setLabel("StartLog");
		System.out.println("ログ出力終了：" + file_name);
	}

	public void keyPressed(KeyEvent e) {
		sendCommand(e.getKeyCode());
		System.out.println("Pressed " + e.getKeyCode());
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent arg0) {
	}
}

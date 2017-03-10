package com.thor.chess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

public class BluetoothNetEngine extends NetEngine {

	private BluetoothAdapter bluetoothAdapter = null;

	private BluetoothSocket sock = null;

	private BluetoothServerSocket listenSock = null;

	private InputStream input = null;

	private OutputStream output = null;

	public BluetoothNetEngine(String yourName, int playerColor) {
		super(yourName, playerColor);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	public BluetoothNetEngine(String yourName, String remoteAddress) {
		super(yourName, remoteAddress);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	protected void initInputOutput(boolean isServer, int playerColor, String playerName) throws IOException {
		input = sock.getInputStream();
		output = sock.getOutputStream();
		if (isServer) {
			String msg = String.format("ask:start:%1$s\n", playerColor);
			sendToPartner(msg);
		}
		// Send your name to your partner.
		String syncMessage = String.format("ask:sync:%1$s\n", playerName);
		sendToPartner(syncMessage);
	}

	@Override
	public void connect(final IConnectListener listener) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				// Connect to remote server
				BluetoothDevice device = bluetoothAdapter
						.getRemoteDevice(remoteAddress);
				try {
					sock = device.createRfcommSocketToServiceRecord(CHESS_UUID);
					sock.connect();
					listener.onCompleted(true);

					BluetoothNetEngine.this.run(false); // Run net engine.

				} catch (IOException e) {
					listener.onCompleted(false);
				}
			}
		});
		thread.start();
	}

	@Override
	public void stopListen() {
		try {
			listenSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void sendToPartner(String msg) {
		if (output != null) {
			try {
				output.write(msg.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				try {
					sock.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	@Override
	protected Iterator<NetCommand> readCommand() {
		List<NetCommand> cmdList = new ArrayList<NetCommand>();
		try {
			byte[] buffer = new byte[4096];
			int contentSize = 0;
			int readSize = 0;

			readSize = input.read(buffer, contentSize, buffer.length
					- contentSize);
			if (readSize > 0) {
				contentSize += readSize;
				NetCommand cmd = parseCommand(buffer, contentSize);
				while (cmd != null) {
					contentSize -= cmd.pos;
					System.arraycopy(buffer, cmd.pos, buffer, 0, contentSize);
					cmdList.add(cmd);
					cmd = parseCommand(buffer, contentSize);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			cmdList = new ArrayList<NetCommand>();
		}
		return cmdList.iterator();
	}

	@Override
	public void listen(final IAcceptListener listener) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					listenSock = bluetoothAdapter
							.listenUsingRfcommWithServiceRecord("ChineseChess",
									CHESS_UUID);
					sock = listenSock.accept();
					remoteAddress = sock.getRemoteDevice().getAddress();
					listenSock.close();
					listener.onCompleted(true);
					BluetoothNetEngine.this.run(true); // Run net engine.
				} catch (IOException e) {
					listener.onCompleted(false);
				}
			}
		});
		thread.start();
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		try {
			offeredToClose = true;
			if (sock != null) {
				sock.close();
			}
		} catch (IOException e) {
		}
	}
}

package com.thor.chess;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import andren.game.china.chess.R;

class ServerInfo {
	public String title;
	public String ip;
	public String description;
	public boolean isClose;
	private final static Pattern serverAdd = Pattern
			.compile("^s:(.+)\t(.+)\n$");
	private final static Pattern serverRemove = Pattern.compile("^s:\n$");

	public static ServerInfo parse(DatagramPacket packet) {
		if (packet.getLength() > 0) {
			try {
				String cmd = new String(packet.getData(), "UTF-8");
				Matcher matcher = serverAdd.matcher(cmd);
				if (matcher.find()) {
					ServerInfo serverInfo = new ServerInfo();
					serverInfo.title = matcher.group(1);
					serverInfo.description = matcher.group(2);
					serverInfo.ip = packet.getAddress().getHostAddress();
					serverInfo.isClose = false;
					return serverInfo;
				} else {
					matcher = serverRemove.matcher(cmd);
					if (matcher.find()) {
						ServerInfo serverInfo = new ServerInfo();
						serverInfo.isClose = true;
						serverInfo.ip = packet.getAddress().getHostAddress();
						return serverInfo;
					}
				}
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
}

public class BluetoothNetGameDialog extends NetGameDialog {
	private NetAdapter adapter = null;

	private BluetoothAdapter bluetoothAdapter = null;

	private ProgressDialog progressDialog = null;

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					String address = device.getAddress();
					String name = addressToName(address, device.getName());
					if (name.trim().length() == 0) {
						name = address;
					}
					data.add(new ListItem(name, address, "未配对"));
					adapter.notifyDataSetChanged();
				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				closeProgressBar();
			}
		}
	};

	private void closeProgressBar() {
		progressDialog.cancel();
	}

	private void showProgressBar() {
		progressDialog = ProgressDialog.show(context, null, "正在搜索设备...", true,
				true, new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						if (bluetoothAdapter.isDiscovering())
							bluetoothAdapter.cancelDiscovery();
					}
				});
	}

	public BluetoothNetGameDialog(Context context) {
		super(context);
		this.context = context;
	}


	class ListItem extends HashMap<String, String> {
		private static final long serialVersionUID = -891461887415985079L;

		public ListItem(String title, String ip, String info) {
			put("title", title);
			put("ip", ip);
			put("info", info);
		}
	}

	private String getSelectAddress() {
		if (listView.getTag() == null)
			return null;
		int pos = (Integer) listView.getTag();
		if (pos >= 0 && pos < data.size())
			return data.get(pos).get("ip");
		else
			return null;
	}

	public void startDiscovery() {
		if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter
					.getBondedDevices();
			for (BluetoothDevice device : pairedDevices) {
				String address = device.getAddress();
				String name = addressToName(address, device.getName());
				if (name.trim().length() == 0) {
					name = address;
				}
				data.add(new ListItem(name, address, "配对的"));
			}
			showProgressBar();
			bluetoothAdapter.startDiscovery();
		}
	}

	private static String addressToName(String address, String defaultName) {
		if (defaultName == null)
			defaultName = "";
		return ChessApplication.getSetting(address, defaultName);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.btnet_menu);
		setTitle(R.string.net_game);
		EditText username = (EditText) findViewById(R.id.txt_player_name);
		listView = (ListView) findViewById(R.id.list_games);
		username.setText(ChessApplication.getSetting("Player", ""));

		adapter = new NetAdapter(context, data);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				listView.setTag(position);
			}
		});

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		context.registerReceiver(receiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		context.registerReceiver(receiver, filter);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter != null) {
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter
					.getBondedDevices();
			for (BluetoothDevice device : pairedDevices) {
				String address = device.getAddress();
				String name = addressToName(address, device.getName());
				if (name.trim().length() == 0) {
					name = address;
				}
				data.add(new ListItem(name, address, "配对的"));
			}
		}

		// //////////////////////////////////////////////////////////
		// Setup button events
		Button btnDiscovery = (Button) findViewById(R.id.btn_discovery);
		btnDiscovery.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				data.clear();
				adapter.notifyDataSetChanged();
				if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
					Set<BluetoothDevice> pairedDevices = bluetoothAdapter
							.getBondedDevices();
					for (BluetoothDevice device : pairedDevices) {
						data.add(new ListItem(device.getName(), device
								.getAddress(), "配对的"));
					}
					showProgressBar();
					bluetoothAdapter.startDiscovery();
				} else {
					ensureBluetoothOn();
				}
			}
		});

		final Button btnJoin = (Button) findViewById(R.id.btn_join_host);
		btnJoin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!verifyName())
					return;
				ChessApplication.setSetting("Player", playerName);
				String address = getSelectAddress();
				if (address == null) {
					Toast toast = Toast.makeText(context, "请选择一个设备",
							Toast.LENGTH_SHORT);
					toast.show();
					return;
				}
				final ProgressDialog dialog = ProgressDialog.show(context,
						null, "连接到设备...", true, false);
				final NetEngine engine = new BluetoothNetEngine(playerName,
						address);
				engine.connect(new IConnectListener() {
					public void onCompleted(boolean connectionEstablished) {
						if (connectionEstablished) {
							btnJoin.post(new Runnable() {
								public void run() {
									dialog.cancel();
									dismiss();
									if (listener != null)
										listener.onCreateEngine(engine, false);
								}
							});
						} else {
							btnJoin.post(new Runnable() {
								public void run() {
									dialog.cancel();
									Toast toast = Toast.makeText(context,
											"无法连接到设备", Toast.LENGTH_SHORT);
									toast.show();
								}
							});
						}
					}
				});
			}
		});

		Button btnNew = (Button) findViewById(R.id.btn_new_host);
		btnNew.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!verifyName())
					return;
				ChessApplication.setSetting("Player", playerName);

				if (bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
					startNetServer(true);

				} else {
					ensureDiscoverable();
				}
			}
		});

		setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				if (bluetoothAdapter.isDiscovering())
					bluetoothAdapter.cancelDiscovery();
				context.unregisterReceiver(receiver);
			}
		});

	}

	private void ensureBluetoothOn() {
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			((Activity) context).startActivityForResult(enableIntent, 1);
		}
	}

	private void ensureDiscoverable() {
		if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			((Activity) context).startActivityForResult(discoverableIntent, 2);
		}
	}

	private boolean verifyName() {
		EditText username = (EditText) findViewById(R.id.txt_player_name);
		playerName = username.getText().toString().trim();
		byte[] nameBytes = playerName.getBytes();
		if (playerName.length() == 0 || nameBytes.length > 24) {
			Toast toast = Toast.makeText(context, "请输入昵称,别超过8个汗字",
					Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, -100);
			toast.show();
			return false;
		} else
			return true;
	}

	@Override
	protected NetEngine createNetEngine(String playerName, int playerColor) {
		return new BluetoothNetEngine(playerName, playerColor);
	}
}

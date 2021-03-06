package com.gentlemansoftware.pixelworld.menu;

import java.util.LinkedList;
import java.util.List;

import com.gentlemansoftware.easyServer.EasyRunnableParameters;
import com.gentlemansoftware.easyServer.EasyRunnableParametersInterface;
import com.gentlemansoftware.pixelworld.helper.MyTextInputListener;
import com.gentlemansoftware.pixelworld.menuComponents.ChatOverlay;
import com.gentlemansoftware.pixelworld.simplemenu.SimpleMenu;
import com.gentlemansoftware.pixelworld.simplemenu.SimpleMenuComponent;
import com.gentlemansoftware.pixelworld.simplemenu.SimpleMenuNameTypes;
import com.gentlemansoftware.pixelworld.simplemenu.SimpleMenuRunnableItem;
import com.gentlemansoftware.pixelworld.simplemenu.SimpleMenuVarholder;

public class MultiplayerMenu extends SimpleMenu {

	ChatOverlay chatoverlay;
	SimpleMenuRunnableItem startAndConnectToLocal, connectToServer, hostLocal, messageItem, disconnectItem;
	private MyTextInputListener inputListener;
	SimpleMenuVarholder serverOptions;

	public MultiplayerMenu(MenuHandler handler, Menu parent) {
		super(handler, parent, "Multiplayer", null);
		inputListener = new MyTextInputListener(createRunnableSendMessage(), "Connect to IP", "", "Ip Adress");
		this.setContent(initMenuComponents());
		
		userIsNotConnected();
	}

	private EasyRunnableParametersInterface<String> createRunnableSendMessage() {
		EasyRunnableParametersInterface<String> aRunnable = new EasyRunnableParameters<String>() {
			public void run() {
				String message = this.getParam();
				handler.user.network.connectTo(message);
			}
		};

		return aRunnable;
	}

	public void removeAllConents() {
		this.removeContent(hostLocal);
		this.removeContent(connectToServer);
		this.removeContent(disconnectItem);
		this.removeContent(messageItem);
		this.removeContent(startAndConnectToLocal);
		this.removeContent(serverOptions);
	}

	public void userIsConnected() {
		removeAllConents();
		this.addContent(disconnectItem);
		this.addContent(messageItem);
		
		serverOptions = new SimpleMenuVarholder(handler, this, "Server Options",handler.user.network.gameServer.server.variables.getVars());
		this.addContent(serverOptions);
	}

	public void userIsNotConnected() {
		removeAllConents();
		this.addContent(startAndConnectToLocal);
		this.addContent(hostLocal);
		this.addContent(connectToServer);
	}

	public List<SimpleMenuComponent> initMenuComponents() {
		List<SimpleMenuComponent> menuComponents = new LinkedList<SimpleMenuComponent>();

		Runnable playLocalRunnable = new Runnable() {
			public void run() {
				handler.user.network.hostServer();

				Thread thread = new Thread() {
					public void run() {
						handler.user.network.connectTo("localhost");
					}
				};

				thread.start();

			}
		};
		startAndConnectToLocal = new SimpleMenuRunnableItem("Play Local", SimpleMenuNameTypes.SUB, playLocalRunnable);

		Runnable connectRunnable = new Runnable() {
			public void run() {
				inputListener.getInput();
			}
		};
		connectToServer = new SimpleMenuRunnableItem("Connect", SimpleMenuNameTypes.SUB, connectRunnable);

		Runnable hostRunnable = new Runnable() {
			public void run() {
				handler.user.network.hostServer();
			}
		};
		hostLocal = new SimpleMenuRunnableItem("Host", SimpleMenuNameTypes.SUB, hostRunnable);

		Runnable messageRunnable = new Runnable() {
			public void run() {
				handler.user.network.gameClient.waitForTextInputAndSendMessage();
			}
		};
		messageItem = new SimpleMenuRunnableItem("Message", SimpleMenuNameTypes.SUB, messageRunnable);

		Runnable disconnectRunnable = new Runnable() {
			public void run() {
				handler.user.network.disconnect();
			}
		};
		disconnectItem = new SimpleMenuRunnableItem("Disconnect", SimpleMenuNameTypes.SUB, disconnectRunnable);

		chatoverlay = new ChatOverlay(this.handler);
		this.addNoChainContent(chatoverlay);

		menuComponents.add(parent);

		return menuComponents;
	}

}

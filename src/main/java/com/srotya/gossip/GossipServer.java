package com.srotya.gossip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GossipServer implements Runnable {
	
	private static final Logger logger = Logger.getLogger(GossipServer.class.getCanonicalName());
	private static final int PACKET_PAYLOAD_SIZE = 4; // 8 bytes for 1 long string
	private AtomicBoolean loopControl = new AtomicBoolean(true);
	private AtomicInteger timer = new AtomicInteger(0);
	private Map<InetAddress, Long> peers = new ConcurrentHashMap<>();
	private ExecutorService esReceiver = Executors.newSingleThreadExecutor();
	private InetAddress address;
	private int port;
	private Random rand = new Random();

	public GossipServer(String bindAddress, int port, int initialTimer) throws UnknownHostException {
		this.address = InetAddress.getByName(bindAddress);
		this.port = port;
		this.timer.set(initialTimer);
	}
	
	protected void startReceptionServer(final DatagramSocket dgSocket) {
		esReceiver.submit(new Runnable() {
			
			@Override
			public void run() {
				receptionLoop(dgSocket);
			}
		});
	}
	
	protected void startTransmissionServer(final DatagramSocket dgSocket, int destPort) throws SocketException, InvalidStateException, InterruptedException {
		try{
			logger.info("Starting Gossip transmission server");
			byte[] buffer = new byte[PACKET_PAYLOAD_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, PACKET_PAYLOAD_SIZE);
			while(loopControl.get()) {
				// send gossip
				packet.setPort(destPort);
				for(InetAddress peer:peers.keySet()) {
					for(InetAddress unicast:peers.keySet()) {
						try {
							packet.setAddress(peer);
							packet.setData(unicast.getAddress());
							dgSocket.send(packet);
						} catch (IOException e) {
							logger.log(Level.SEVERE, "Failed to send gossip packet", e);
						}
					}
				}
				Thread.sleep(timer.get()+rand.nextInt(100));
			}
		}finally{
			dgSocket.close();
		}
	}
	
	protected void receptionLoop(final DatagramSocket dgSocket) {
		byte[] buffer = new byte[PACKET_PAYLOAD_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, PACKET_PAYLOAD_SIZE);
		while(loopControl.get()) {
			try {
				dgSocket.receive(packet);
				byte[] data = packet.getData();
				if(!peers.containsKey(packet.getAddress())) {
					logger.info("Added direct peer:"+packet.getAddress().getHostAddress());
				}
				peers.put(packet.getAddress(), System.currentTimeMillis());
				InetAddress payloadAddress = InetAddress.getByAddress(data);
				if(!peers.containsKey(payloadAddress)) {
					logger.info("Discovered new peer:"+payloadAddress.getHostAddress());
					peers.put(payloadAddress, -1L);
				}
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Error receiving gossip packet", e);
			}
		}
	}

	@Override
	public void run() {
		try {
			if(peers.size()==0) {
				throw new InvalidStateException("Either multicast needs to be turned on or a seed of unicast must be provided");
			}
			final DatagramSocket dgSocket = new DatagramSocket(port, address);
			dgSocket.setTrafficClass(0x04);
			startTransmissionServer(dgSocket, port);
		} catch (SocketException | InvalidStateException e) {
			logger.log(Level.SEVERE, "Exception starting server", e);
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Broadcast loop interrupted", e);
		}
		port = -1;
	}
	
	public void stop(boolean wait) throws InterruptedException {
		loopControl.set(false);
		esReceiver.shutdown();
		while(wait) {
			if(port==-1) {
				return;
			}else{
				Thread.sleep(100);
			}
		}
		esReceiver.shutdownNow();
	}
	
	public void addKnownPeer(String peer) throws UnknownHostException {
		peers.put(InetAddress.getByName(peer), -1L);
	}

	public Set<InetAddress> getPeers() {
		return peers.keySet();
	}
	
}

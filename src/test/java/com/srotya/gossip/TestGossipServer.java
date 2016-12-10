/**
 * Copyright 2015 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.gossip;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestGossipServer {

	@Test
	public void testTransmissionReceptionLogic() throws UnknownHostException, SocketException, InvalidStateException, InterruptedException {
		GossipServer server = new GossipServer("0.0.0.0", 50002, 1000);
		server.addKnownPeer(Inet4Address.getLocalHost().getHostAddress());
		DatagramSocket dgsSend = new DatagramSocket(50002);
		DatagramSocket dgsReceive = new DatagramSocket(50003);
		
		GossipServer server2 = new GossipServer("0.0.0.0", 50003, 1000);
		server2.startReceptionServer(dgsReceive);
		Executors.newSingleThreadExecutor().submit(new Runnable() {
			
			@Override
			public void run() {
				try {
					server.startTransmissionServer(dgsSend, 50003);
				} catch (SocketException | InvalidStateException | InterruptedException e) {
					fail("Failed unit test due to error");
				}
			}
		});
		Thread.sleep(200);
		System.err.println(server2.getPeers());
		assertTrue(server2.getPeers().contains(InetAddress.getByName(Inet4Address.getLocalHost().getHostAddress())));
	}
	
}

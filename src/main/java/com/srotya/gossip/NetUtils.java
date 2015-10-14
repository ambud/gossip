package com.srotya.gossip;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetUtils {
	
	private NetUtils() {
	}
	
	/**
	 * Find broadcast address for multicast gossip implementations
	 * @return
	 * @throws SocketException
	 */
	public static InetAddress getBroadcastAddress() throws SocketException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while(interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();
			if(networkInterface.isLoopback() || !networkInterface.supportsMulticast()) {
				continue;
			}
			for(InterfaceAddress interfaceAddress:networkInterface.getInterfaceAddresses()) {
				InetAddress broadcast = interfaceAddress.getBroadcast();
				if (broadcast == null) {
					continue;
				}else {
					return broadcast;
				}
			}
		}
		return null;
	}
	
	public static byte[] longToBytes(long l) {
	    byte[] result = new byte[8];
	    for (int i = 7; i >= 0; i--) {
	        result[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
	    return result;
	}

	public static long bytesToLong(byte[] b) {
	    long result = 0;
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (b[i] & 0xFF);
	    }
	    return result;
	}

}

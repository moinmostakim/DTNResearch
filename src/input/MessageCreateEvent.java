/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import core.DTNHost;
import core.Message;
import core.World;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	
	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Where the message is destined to
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 */
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
	}

	
	/**
	 * Creates the message this event represents. 
	 */
	@Override
	public void processEvent(World world) {
		DTNHost to = world.getNodeByAddress(this.getToAddr());
		DTNHost from = world.getNodeByAddress(this.getFromAddr());			
		
		Message m = new Message(from, to, this.getId(), this.getSize());
		m.setResponseSize(this.getResponseSize());
		from.createNewMessage(m);
	}
	
	@Override
	public String toString() {
		return super.toString() + " [" + getFromAddr() + "->" + getToAddr() + "] " +
		"size:" + getSize() + " CREATE";
	}

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @return the responseSize
     */
    public int getResponseSize() {
        return responseSize;
    }

    /**
     * @param responseSize the responseSize to set
     */
    public void setResponseSize(int responseSize) {
        this.responseSize = responseSize;
    }
}

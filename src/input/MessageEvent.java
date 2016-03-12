/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

/**
 * A message related external event
 */
public abstract class MessageEvent extends ExternalEvent {
	/** address of the node the message is from */
	private int fromAddr;
	/** address of the node the message is to */
	private int toAddr;
	/** identifier of the message */
	private String id;
	
	/**
	 * Creates a message  event
	 * @param from Where the message comes from
	 * @param to Who the message goes to 
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 */
	public MessageEvent(int from, int to, String id, double time) {
		super(time);
		this.fromAddr = from;
		this.toAddr= to;
		this.id = id;
	}
	
	@Override
	public String toString() {
		return "MSG @" + this.time + " " + getId();
	}

    /**
     * @return the fromAddr
     */
    public int getFromAddr() {
        return fromAddr;
    }

    /**
     * @param fromAddr the fromAddr to set
     */
    public void setFromAddr(int fromAddr) {
        this.fromAddr = fromAddr;
    }

    /**
     * @return the toAddr
     */
    public int getToAddr() {
        return toAddr;
    }

    /**
     * @param toAddr the toAddr to set
     */
    public void setToAddr(int toAddr) {
        this.toAddr = toAddr;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
}

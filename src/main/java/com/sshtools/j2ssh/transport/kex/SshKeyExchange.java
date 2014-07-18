//Changes (c) CCLRC 2006
/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002 Lee David Painter.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */

package com.sshtools.j2ssh.transport.kex;

import java.io.IOException;
import java.math.BigInteger;

import org.ietf.jgss.GSSContext;

import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.transport.TransportProtocol;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;
/**
 *
 *
 * @author $author$
 * @version $Revision: 1.6 $
 */
public abstract class SshKeyExchange { //implements Runnable {

  /**  */
  protected BigInteger secret;

  /**  */
  //protected SshMessageStore messageStore = new SshMessageStore();

  /**  */
  protected byte[] exchangeHash;

  /**  */
  protected byte[] hostKey;

  /**  */
  protected byte[] signature;

  /**  */
  protected TransportProtocol transport;

  /**
   * Creates a new SshKeyExchange object.
   */
  public SshKeyExchange() {
  }

  /**
   *
   *
   * @return
   */
  public byte[] getExchangeHash() {
    return exchangeHash;
  }

  /**
   *
   *
   * @return
   */
  public byte[] getHostKey() {
    return hostKey;
  }

  /**
   *
   *
   * @return
   */
  public BigInteger getSecret() {
    return secret;
  }

  /**
   *
   *
   * @return
   */
  public byte[] getSignature() {
    return signature;
  }

  /**
   *
   *
   * @return
   */
  public GSSContext getGSSContext() {
    return gssContext;
  }
  /**
   *
   *
   * @param transport
   *
   * @throws IOException
   */
  public void init(TransportProtocol transport, String hostname) throws IOException {
    this.transport = transport;
    this.hostname = hostname;
    onInit();
    //transport.addMessageStore(messageStore);
  }

    protected String hostname;
    protected GSSContext gssContext;

  /**
   *
   *
   * @throws IOException
   */
  protected abstract void onInit() throws IOException;

  /**
   *
   *
   * @param clientId
   * @param serverId
   * @param clientKexInit
   * @param serverKexInit
   *
   * @throws IOException
   */
  public abstract void performClientExchange(String clientId,
                                             String serverId,
                                             byte[] clientKexInit,
                                             byte[] serverKexInit,
                                             boolean firstPacketFollows,
                                             boolean useFirstPacket,
					     boolean firstExch) throws
      IOException;

  /**
   *
   *
   * @param clientId
   * @param serverId
   * @param clientKexInit
   * @param serverKexInit
   * @param prvkey
   *
   * @throws IOException
   */
  public abstract void performServerExchange(String clientId,
                                             String serverId,
                                             byte[] clientKexInit,
                                             byte[] serverKexInit,
                                             SshPrivateKey prvkey,
                                             boolean firstPacketFollows,
                                             boolean useFirstPacket) throws
      IOException;

  /**
   *
   */
  public void reset() {
    exchangeHash = null;
    hostKey = null;
    signature = null;
    secret = null;
    gssContext = null;
  }
    protected SshConnectionProperties properties;
    void setProperties(SshConnectionProperties e) { properties=e;}
}

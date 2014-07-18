/*
 *  GSI-SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2005-6 CCLRC.
 *
 *  Based on SshMsgKexDh* (c) 2002 Lee David Painter.
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

import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.transport.InvalidMessageException;
import com.sshtools.j2ssh.transport.SshMessage;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.2 $
 */
public class SshMsgKexGssError
    extends SshMessage {
  /**  */
  protected final static int SSH_MSG_KEXGSS_ERROR = 34;

    private int major, minor;
    private String message, lang;

  /**
   * Creates a new SshMsgKexGssError object.
   *
   * @param token
   */
  public SshMsgKexGssError(int major, int minor, String message, String lang) {
    super(SSH_MSG_KEXGSS_ERROR);
    this.major = major;
    this.minor = minor;
    this.message = message;
    this.lang = lang;
  }

  /**
   * Creates a new SshMsgKexGssInit object.
   */
  public SshMsgKexGssError() {
    super(SSH_MSG_KEXGSS_ERROR);
  }

    public int getMinor() {
	return minor;
    }
    public String getMessage() {
	return message;
    }
    public String getLangage() {
	return lang;
    }
    public int getMajor() {
	return major;
    }

  /**
   *
   *
   * @return
   */
  public String getMessageName() {
    return "SSH_MSG_KEXGSS_ERROR";
  }

  /**
   *
   *
   * @param baw
   *
   * @throws InvalidMessageException
   */
  protected void constructByteArray(ByteArrayWriter baw) throws
      InvalidMessageException {
    try {
	baw.writeInt(major);
	baw.writeInt(minor);
	
	baw.writeString(message);
	baw.writeString(lang);
    }
    catch (IOException ioe) {
      throw new InvalidMessageException("Error writing message data: "
                                        + ioe.getMessage());
    }
  }

  /**
   *
   *
   * @param bar
   *
   * @throws InvalidMessageException
   */
  protected void constructMessage(ByteArrayReader bar) throws
      InvalidMessageException {
    try {
	major = (int) bar.readInt();
	minor = (int) bar.readInt();
	message = bar.readString();
	lang = bar.readString();
    }
    catch (IOException ioe) {
      throw new InvalidMessageException("Error reading message data: "
                                        + ioe.getMessage());
    }
  }
}

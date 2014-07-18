/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sshtools.j2ssh.authentication;

import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.transport.InvalidMessageException;
import com.sshtools.j2ssh.transport.SshMessage;
import java.io.IOException;

public class SshMsgUserauthGssapiMIC extends SshMessage
{
    private byte[] mic;
    public static final int SSH_MSG_USERAUTH_GSSAPI_MIC = 66;

    public SshMsgUserauthGssapiMIC(byte[] mic)
    {
        super(SSH_MSG_USERAUTH_GSSAPI_MIC);
        this.mic = mic;
    }

    public String getMessageName()
    {
        return "SSH_MSG_USERAUTH_GSSAPI_MIC";
    }

    protected void constructByteArray(ByteArrayWriter baw)
        throws InvalidMessageException
    {
      try {
        baw.writeBinaryString(mic);
      }
      catch (IOException ioe) {
        throw new InvalidMessageException("Invalid message data");
      }
    }

    protected void constructMessage(ByteArrayReader bar)
        throws InvalidMessageException
    {
      try {
        if (bar.available() > 0) {
          mic = new byte[bar.available()];
          bar.read(mic);
        }
      }
      catch (IOException ioe) {
        throw new InvalidMessageException("Invalid message data");
      }
    }
}

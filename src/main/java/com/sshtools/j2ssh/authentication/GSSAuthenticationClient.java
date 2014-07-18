/**
 * Copyright (c) 2004, National Research Council of Canada
 * All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this 
 * software and associated documentation files (the "Software"), to deal in the Software 
 * without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice(s) and this licence appear in all copies of the Software or 
 * substantial portions of the Software, and that both the above copyright notice(s) and this 
 * license appear in supporting documentation.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE BE LIABLE 
 * FOR ANY CLAIM, OR ANY DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL 
 * DAMAGES, OR ANY DAMAGES WHATSOEVER (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS 
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWSOEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OF THE SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Except as contained in this notice, the name of a copyright holder shall NOT be used in 
 * advertising or otherwise to promote the sale, use or other dealings in this Software 
 * without specific prior written authorization.  Title to copyright in this software and any 
 * associated documentation will at all times remain with copyright holders.
 */
package com.sshtools.j2ssh.authentication;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.GlobusGSSManagerImpl;
import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.gridforum.jgss.ExtendedGSSContext;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.sshtools.common.configuration.SshToolsConnectionProfile;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.io.UnsignedInteger32;

import edu.illinois.ncsa.BCGSS.BCGSSContextImpl;

public class GSSAuthenticationClient extends SshAuthenticationClient
{
	private String ukcaCert = "01621954.0";
	private String ukcaSigningPolicy = "01621954.signing_policy";
	private GSSCredential gsscredential;

    public GSSAuthenticationClient(GSSCredential gsscredential) {
		this.gsscredential = gsscredential;
		// TODO Auto-generated constructor stub
	}

	public final String getMethodName()
    {
        //return "gssapi";
         return "gssapi-with-mic";
    }

    public void reset()
    {
    }
    

    public void authenticate(AuthenticationProtocolClient authenticationprotocolclient, String s)
        throws IOException, TerminatedStateException
    {
        try
        {
            log.debug("Registering gss-ssh return messages.");
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiResponse.class, 60);
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiToken.class, 61);
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiError.class, 64);
            authenticationprotocolclient.registerMessage(com.sshtools.j2ssh.authentication.SshMsgUserauthGssapiErrtok.class, 65);
            log.debug("Sending gssapi user auth request.");
            ByteArrayWriter bytearraywriter = new ByteArrayWriter();
            bytearraywriter.writeUINT32(new UnsignedInteger32(1L));
            byte abyte0[] = GSSConstants.MECH_OID.getDER();
            bytearraywriter.writeBinaryString(abyte0);

            //SshMsgUserAuthRequest sshmsguserauthrequest = new SshMsgUserAuthRequest(getUsername(), s, "gssapi", bytearraywriter.toByteArray());
            SshMsgUserAuthRequest sshmsguserauthrequest = new SshMsgUserAuthRequest(getUsername(), s, "gssapi-with-mic", bytearraywriter.toByteArray());
            authenticationprotocolclient.sendMessage(sshmsguserauthrequest);
            log.debug("Receiving user auth response.");
            SshMsgUserauthGssapiResponse sshmsguserauthgssapiresponse = (SshMsgUserauthGssapiResponse)authenticationprotocolclient.readMessage(60);
            ByteArrayReader bytearrayreader = new ByteArrayReader(sshmsguserauthgssapiresponse.getRequestData());
            byte abyte1[] = bytearrayreader.readBinaryString();
            log.debug("Mechanism requested: " + GSSConstants.MECH_OID);
            log.debug("Mechanism selected: " + new Oid(abyte1));
            log.debug("Verify that selected mechanism is GSSAPI.");
            if(!GSSConstants.MECH_OID.equals(new Oid(abyte1)))
            {
                log.debug("Mechanism do not match!");
                throw new IOException("Mechanism do not match!");
            }
            log.debug("Creating GSS context base on grid credentials.");
            GlobusGSSManagerImpl globusgssmanagerimpl = new GlobusGSSManagerImpl();

	    HostAuthorization gssAuth = new HostAuthorization(null);
	    GSSName targetName = gssAuth.getExpectedName(null, hostname);

            GSSContext gsscontext = new BCGSSContextImpl(targetName, (GlobusGSSCredentialImpl) gsscredential);
            gsscontext.requestCredDeleg(true);
            gsscontext.requestMutualAuth(true);
            gsscontext.requestReplayDet(true);
            gsscontext.requestSequenceDet(true);
            gsscontext.requestConf(false);
            gsscontext.requestInteg(true);
	    Object type = GSIConstants.DELEGATION_TYPE_LIMITED;
	    String cur = "None";
	    if(properties instanceof SshToolsConnectionProfile) {
		cur = "Full";
		if(cur.equals("full")) {
		    type = GSIConstants.DELEGATION_TYPE_FULL;
		} else if(cur.equals("limited")) {
		    type = GSIConstants.DELEGATION_TYPE_LIMITED;	
		} else if(cur.equals("none")) {
		    type = GSIConstants.DELEGATION_TYPE_LIMITED;
		    gsscontext.requestCredDeleg(false);	
		}
		log.debug("Enabling delegation setting: "+cur);
	    }
	    ((ExtendedGSSContext)gsscontext).setOption(GSSConstants.DELEGATION_TYPE, type);

            log.debug("Starting GSS token exchange.");
            byte abyte2[] = new byte[0];
            Object obj = null;
            do
            {
            	if(gsscontext.isEstablished())
                    break;
                byte abyte3[] = gsscontext.initSecContext(abyte2, 0, abyte2.length);
                if(abyte3 != null)
                {
                    ByteArrayWriter bytearraywriter1 = new ByteArrayWriter();
                    bytearraywriter1.writeBinaryString(abyte3);
                    SshMsgUserauthGssapiToken sshmsguserauthgssapitoken = new SshMsgUserauthGssapiToken(bytearraywriter1.toByteArray());
                    authenticationprotocolclient.sendMessage(sshmsguserauthgssapitoken);
                }
                if(!gsscontext.isEstablished())
                {
                    SshMsgUserauthGssapiToken sshmsguserauthgssapitoken1 = (SshMsgUserauthGssapiToken)authenticationprotocolclient.readMessage(61);
                    ByteArrayReader bytearrayreader1 = new ByteArrayReader(sshmsguserauthgssapitoken1.getRequestData());
                    abyte2 = bytearrayreader1.readBinaryString();
                }
            } while(true);
            //log.debug("Sending gssapi exchange complete.");
            //SshMsgUserauthGssapiExchangeComplete sshmsguserauthgssapiexchangecomplete = new SshMsgUserauthGssapiExchangeComplete();
            //authenticationprotocolclient.sendMessage(sshmsguserauthgssapiexchangecomplete);

            ByteArrayWriter baw = new ByteArrayWriter();
            baw.writeBinaryString(authenticationprotocolclient.getSessionIdentifier());
            baw.write(SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST);
            baw.writeString(getUsername());
            baw.writeString(s);
            baw.writeString("gssapi-with-mic");

            log.debug("Sending gssapi MIC.");
            SshMsgUserauthGssapiMIC micMsg = new SshMsgUserauthGssapiMIC(
               gsscontext.getMIC(baw.toByteArray(), 0, baw.toByteArray().length, null)
            );
            authenticationprotocolclient.sendMessage(micMsg);


            log.debug("Context established.");
            log.debug("Initiator : " + gsscontext.getSrcName());
            log.debug("Acceptor  : " + gsscontext.getTargName());
            log.debug("Lifetime  : " + gsscontext.getLifetime());
            log.debug("Privacy   : " + gsscontext.getConfState());
            log.debug("Anonymity : " + gsscontext.getAnonymityState());
            log.debug("Integrity : " + gsscontext.getIntegState());

        }
        catch(GSSException gssexception)
        {
            gssexception.printStackTrace();
            StringWriter stringwriter = new StringWriter();
            gssexception.printStackTrace(new PrintWriter(stringwriter));
            log.debug(stringwriter);
        } catch (Exception e) {			
			e.printStackTrace();
		}
    }

    public Properties getPersistableProperties()
    {
        Properties properties = new Properties();
        return properties;
    }

    public void setPersistableProperties(Properties properties)
    {
    }

    public boolean canAuthenticate()
    {
        return true;
    }


    private static Log log;

    static 
    {
        log = LogFactory.getLog(com.sshtools.j2ssh.authentication.GSSAuthenticationClient.class);
    }
}

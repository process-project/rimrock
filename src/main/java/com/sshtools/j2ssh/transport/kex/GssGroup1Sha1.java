/*
 *  GSI-SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2005-7 STFC/CCLRC.
 *
 *  Based on DhGroup1Sha1.java (c) 2002 Lee David Painter.
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.gsi.CredentialException;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.gridforum.jgss.ExtendedGSSContext;
import org.ietf.jgss.GSSContext;
//import org.globus.tools.proxy.DefaultGridProxyModel;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;

import com.sshtools.common.configuration.SshToolsConnectionProfile;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;
import com.sshtools.j2ssh.transport.AlgorithmOperationException;
import com.sshtools.j2ssh.transport.SshMessage;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;
import com.sshtools.j2ssh.util.Hash;

import edu.illinois.ncsa.BCGSS.BCGSSContextImpl;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class GssGroup1Sha1 extends SshKeyExchange {
	private static Log log = LogFactory.getLog(GssGroup1Sha1.class);
	private static BigInteger g = new BigInteger("2");
	private static BigInteger p = new BigInteger(new byte[] {
			(byte) 0x00, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF,
			(byte) 0xC9,
			(byte) 0x0F, (byte) 0xDA,
			(byte) 0xA2, (byte) 0x21,
			(byte) 0x68,
			(byte) 0xC2, (byte) 0x34,
			(byte) 0xC4, (byte) 0xC6,
			(byte) 0x62,
			(byte) 0x8B, (byte) 0x80,
			(byte) 0xDC, (byte) 0x1C,
			(byte) 0xD1,
			(byte) 0x29, (byte) 0x02,
			(byte) 0x4E, (byte) 0x08,
			(byte) 0x8A,
			(byte) 0x67, (byte) 0xCC,
			(byte) 0x74, (byte) 0x02,
			(byte) 0x0B,
			(byte) 0xBE, (byte) 0xA6,
			(byte) 0x3B, (byte) 0x13,
			(byte) 0x9B,
			(byte) 0x22, (byte) 0x51,
			(byte) 0x4A, (byte) 0x08,
			(byte) 0x79,
			(byte) 0x8E, (byte) 0x34,
			(byte) 0x04, (byte) 0xDD,
			(byte) 0xEF,
			(byte) 0x95, (byte) 0x19,
			(byte) 0xB3, (byte) 0xCD,
			(byte) 0x3A,
			(byte) 0x43, (byte) 0x1B,
			(byte) 0x30, (byte) 0x2B,
			(byte) 0x0A,
			(byte) 0x6D, (byte) 0xF2,
			(byte) 0x5F, (byte) 0x14,
			(byte) 0x37,
			(byte) 0x4F, (byte) 0xE1,
			(byte) 0x35, (byte) 0x6D,
			(byte) 0x6D,
			(byte) 0x51, (byte) 0xC2,
			(byte) 0x45, (byte) 0xE4,
			(byte) 0x85,
			(byte) 0xB5, (byte) 0x76,
			(byte) 0x62, (byte) 0x5E,
			(byte) 0x7E,
			(byte) 0xC6, (byte) 0xF4,
			(byte) 0x4C, (byte) 0x42,
			(byte) 0xE9,
			(byte) 0xA6, (byte) 0x37,
			(byte) 0xED, (byte) 0x6B,
			(byte) 0x0B,
			(byte) 0xFF, (byte) 0x5C,
			(byte) 0xB6, (byte) 0xF4,
			(byte) 0x06,
			(byte) 0xB7, (byte) 0xED,
			(byte) 0xEE, (byte) 0x38,
			(byte) 0x6B,
			(byte) 0xFB, (byte) 0x5A,
			(byte) 0x89, (byte) 0x9F,
			(byte) 0xA5,
			(byte) 0xAE, (byte) 0x9F,
			(byte) 0x24, (byte) 0x11,
			(byte) 0x7C,
			(byte) 0x4B, (byte) 0x1F,
			(byte) 0xE6, (byte) 0x49,
			(byte) 0x28,
			(byte) 0x66, (byte) 0x51,
			(byte) 0xEC, (byte) 0xE6,
			(byte) 0x53,
			(byte) 0x81, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF
	});
	private BigInteger e = null;
	private BigInteger f = null;

	//private static BigInteger q = p.subtract(BigInteger.ONE).divide(g);
	private BigInteger x = null;
	private BigInteger y = null;
	private String clientId;
	private String serverId;
	private byte[] clientKexInit;
	private byte[] serverKexInit;
	private KeyPairGenerator dhKeyPairGen;
	private KeyAgreement dhKeyAgreement;

	private GSSCredential theCredential=null;

	/**
	 * Creates a new GssGroup1Sha1 object.
	 */
	public GssGroup1Sha1() {
	}

	/**
	 *
	 *
	 * @throws IOException
	 * @throws AlgorithmNotSupportedException
	 */
	protected void onInit() throws IOException {


		try {
			dhKeyPairGen = KeyPairGenerator.getInstance("DH");
			dhKeyAgreement = KeyAgreement.getInstance("DH");
		}
		catch (NoSuchAlgorithmException ex) {
			throw new AlgorithmNotSupportedException(ex.getMessage());
		}
	}

	/**
	 *
	 *
	 * @param clientId
	 * @param serverId
	 * @param clientKexInit
	 * @param serverKexInit
	 *
	 * @throws IOException
	 * @throws AlgorithmOperationException
	 * @throws KeyExchangeException
	 */
	public void performClientExchange(String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit,
			boolean firstPacketFollows,
			boolean useFirstPacket, boolean firstExch) throws
			IOException {
		try {
			log.info("Starting client side key exchange.");
			transport.getMessageStore().registerMessage(SshMsgKexGssInit.SSH_MSG_KEXGSS_INIT,
					SshMsgKexGssInit.class);

			transport.getMessageStore().registerMessage(SshMsgKexGssContinue.SSH_MSG_KEXGSS_CONTINUE,
					SshMsgKexGssContinue.class);
			transport.getMessageStore().registerMessage(SshMsgKexGssComplete.SSH_MSG_KEXGSS_COMPLETE,
					SshMsgKexGssComplete.class);

			transport.getMessageStore().registerMessage(SshMsgKexGssHostKey.SSH_MSG_KEXGSS_HOSTKEY,
					SshMsgKexGssHostKey.class);
			transport.getMessageStore().registerMessage(SshMsgKexGssError.SSH_MSG_KEXGSS_ERROR,
					SshMsgKexGssError.class);
			this.clientId = clientId;
			this.serverId = serverId;
			this.clientKexInit = clientKexInit;
			this.serverKexInit = serverKexInit;

			//int minBits = g.bitLength();
			//int maxBits = q.bitLength();
			//Random rnd = ConfigurationLoader.getRND();
			// Generate a random bit count for the random x value

			/*int genBits = (int) ( ( (maxBits - minBits + 1) * rnd.nextFloat())
                 + minBits);
          x = new BigInteger(genBits, rnd);
          // Calculate e
          e = g.modPow(x, p);*/
			try {
				DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, g);
				dhKeyPairGen.initialize(dhSkipParamSpec);

				KeyPair dhKeyPair = dhKeyPairGen.generateKeyPair();
				dhKeyAgreement.init(dhKeyPair.getPrivate());
				x = ( (DHPrivateKey) dhKeyPair.getPrivate()).getX();
				e = ( (DHPublicKey) dhKeyPair.getPublic()).getY();
			}
			catch (InvalidKeyException ex) {
				throw new AlgorithmOperationException("Failed to generate DH value");
			}
			catch (InvalidAlgorithmParameterException ex) {
				throw new AlgorithmOperationException("Failed to generate DH value");
			}
			//C calls GSS_Init_sec_context!
			log.info("Generating shared context with server...");
			
			
			GSSCredential gsscredential = null;			

			

			HostAuthorization gssAuth = new HostAuthorization(null);
			GSSName targetName = gssAuth.getExpectedName(null, hostname);
			GSSContext gsscontext = null;
        	  if(theCredential==null) {
        		  gsscredential = properties.getUserProxy();
        		  theCredential = gsscredential;
        	  } else {
        		  gsscredential = theCredential;
        		  
        		  try {
        			  //Cerlane : To support VOMS proxy type 4
        			  ((GlobusGSSCredentialImpl)gsscredential).getX509Credential().verify();

        		  } catch(NullPointerException e) {
        			  e.printStackTrace();
        		  } catch(CredentialException e) {
        			  e.printStackTrace();
        			  gsscredential = properties.getUserProxy();
        			  theCredential = gsscredential;
        		  }
        	  }
        	  
        	  gsscontext = new BCGSSContextImpl(targetName, (GlobusGSSCredentialImpl) gsscredential);
        	  gsscontext.requestLifetime(GSSCredential.DEFAULT_LIFETIME);
        	  gsscontext.requestCredDeleg(true);
        	  gsscontext.requestMutualAuth(true);
        	  gsscontext.requestInteg(true);
        	  //gsscontext.requestAnonymity(false);
        	  // gsscontext.requestReplayDet(false);
        	  //gsscontext.requestSequenceDet(false);
        	  // gsscontext.requestConf(false);
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
        	  }
        	  log.debug("Enabling delegation setting: "+cur);
        	  ((ExtendedGSSContext)gsscontext).setOption(GSSConstants.DELEGATION_TYPE, type);



			log.debug("Starting GSS token exchange.");
			byte abyte2[] = new byte[0];
			Object obj = null;
			boolean firsttime = true;
			hostKey = null;
			do	{
				if(gsscontext.isEstablished())
					break;
				byte abyte3[] = gsscontext.initSecContext(abyte2, 0, abyte2.length);
				if(gsscontext.isEstablished() && !gsscontext.getMutualAuthState()) {
					// bad authenitcation 
					throw new KeyExchangeException("Context established without mutual authentication in gss-group1-sha1-* key exchange.");
				}
				if(gsscontext.isEstablished() && !gsscontext.getIntegState()) {
					// bad authenitcation 
					throw new KeyExchangeException("Context established without integrety protection in gss-group1-sha1-* key exchange.");
				}
				if(abyte3 != null) {
					if(firsttime) {
						SshMsgKexGssInit msg = new SshMsgKexGssInit(e, /*bytearraywriter1.toByteArray()*/abyte3);
						transport.sendMessage(msg, this);
					} else {
						SshMsgKexGssContinue msg = new SshMsgKexGssContinue(/*bytearraywriter1.toByteArray()*/abyte3);
						transport.sendMessage(msg, this);
					}
				} else {
					throw new KeyExchangeException("Expecting a non-zero length token from GSS_Init_sec_context.");
				}
				if(!gsscontext.isEstablished()) {
					int[] messageId = new int[3];
					messageId[0]=SshMsgKexGssHostKey.SSH_MSG_KEXGSS_HOSTKEY;
					messageId[1]=SshMsgKexGssContinue.SSH_MSG_KEXGSS_CONTINUE;
					messageId[2]=SshMsgKexGssError.SSH_MSG_KEXGSS_ERROR;
					SshMessage msg = transport.readMessage(messageId);
					if(msg.getMessageId()==SshMsgKexGssHostKey.SSH_MSG_KEXGSS_HOSTKEY) {
						if(!firsttime) {
							throw new KeyExchangeException("Not expecting a SSH_MSG_KEXGS_HOSTKEY message at this time.");
						}
						SshMsgKexGssHostKey reply = (SshMsgKexGssHostKey) msg;
						hostKey = reply.getHostKey();
						messageId = new int[2];
						messageId[0]=SshMsgKexGssContinue.SSH_MSG_KEXGSS_CONTINUE;
						messageId[1]=SshMsgKexGssError.SSH_MSG_KEXGSS_ERROR;
						msg = transport.readMessage(messageId);
						if(msg.getMessageId()==SshMsgKexGssError.SSH_MSG_KEXGSS_ERROR) errormsg(msg);
					} else if(msg.getMessageId()==SshMsgKexGssError.SSH_MSG_KEXGSS_ERROR) {
						errormsg(msg);
					}
					SshMsgKexGssContinue reply = (SshMsgKexGssContinue) msg;
					abyte2 = reply.getToken();
				}
				firsttime = false;
			} while(true);
			log.debug("Sending gssapi exchange complete.");
			int[] messageId = new int[2];
			messageId[0]=SshMsgKexGssComplete.SSH_MSG_KEXGSS_COMPLETE;
			messageId[1]=SshMsgKexGssError.SSH_MSG_KEXGSS_ERROR;
			SshMessage msg = transport.readMessage(messageId);
			if(msg.getMessageId()==SshMsgKexGssError.SSH_MSG_KEXGSS_ERROR) errormsg(msg);
			SshMsgKexGssComplete reply = (SshMsgKexGssComplete) msg;
			if(reply.hasToken()) {
				ByteArrayReader bytearrayreader1 = new ByteArrayReader(reply.getToken());
				abyte2 = bytearrayreader1.readBinaryString();
				byte abyte3[] = gsscontext.initSecContext(abyte2, 0, abyte2.length);
				if(abyte3!=null) {
					throw new KeyExchangeException("Expecting zero length token.");
				}
				if(gsscontext.isEstablished() && !gsscontext.getMutualAuthState()) {
					// bad authenitcation 
					throw new KeyExchangeException("Context established without mutual authentication in gss-group1-sha1-* key exchange.");
				}
				if(gsscontext.isEstablished() && !gsscontext.getIntegState()) {
					// bad authenitcation 
					throw new KeyExchangeException("Context established without integrety protection in gss-group1-sha1-* key exchange.");
				}
			}

			byte per_msg_token[] = reply.getMIC();
			f = reply.getF();

			// Calculate diffe hellman k value
			secret = f.modPow(x, p);

			// Calculate the exchange hash
			calculateExchangeHash();

			gsscontext.verifyMIC(per_msg_token, 0, per_msg_token.length, exchangeHash, 0, exchangeHash.length, null) ;

			gssContext = gsscontext;
		} catch(GSSException g) {
			String desc = g.toString();
			if(desc.startsWith("GSSException: Failure unspecified at GSS-API level (Mechanism level: GSS Major Status: Authentication Failed") && desc.indexOf("an unknown error occurred")>=0) {
				throw new KeyExchangeException("Error from GSS layer: \n Probably due to your proxy credential being expired or signed by a CA unknown by the server or your clock being set wrong.", g);
			} else {
				if(desc.indexOf("From Server")>=0) {
					throw new KeyExchangeException("GSS Error from server", g);
				} else {
					throw new KeyExchangeException("Error from GSS layer", g);
				}
			}
		} catch(IOException e) {
			//To stop error message to be printed when user cancel login
			if (!e.getMessage().equals("Canceled by user.")){
				e.printStackTrace(); 
			}
			throw e;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void errormsg(SshMessage msg) throws GSSException{
		SshMsgKexGssError m = (SshMsgKexGssError) msg;
		log.error("GSSAPI Error:");
		log.error("Major status: "+m.getMajor());
		log.error("Minor status: "+m.getMinor());
		log.error("Message: "+m.getMessage());
		throw new GSSException(m.getMajor(), m.getMinor(), "\nFrom Server:\n"+m.getMessage()); 
	}

	/**
	 *
	 *
	 * @param clientId
	 * @param serverId
	 * @param clientKexInit
	 * @param serverKexInit
	 * @param prvKey
	 *
	 * @throws IOException
	 * @throws KeyExchangeException
	 */
	public void performServerExchange(String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit,
			SshPrivateKey prvKey,
			boolean firstPacketFollows,
			boolean useFirstPacket) throws IOException {

		throw new KeyExchangeException("gss-group1-sha1-* not yet implemented on server side.");

	}

	/**
	 *
	 *
	 * @throws KeyExchangeException
	 */
	protected void calculateExchangeHash() throws KeyExchangeException {
		Hash hash;

		try {
			// Start a SHA hash
			hash = new Hash("SHA");
		}
		catch (NoSuchAlgorithmException nsae) {
			throw new KeyExchangeException("SHA algorithm not supported");
		}

		int i;

		// The local software version comments
		hash.putString(clientId);

		// The remote software version comments
		hash.putString(serverId);

		// The local kex init payload
		hash.putInt(clientKexInit.length);
		hash.putBytes(clientKexInit);

		// The remote kex init payload
		hash.putInt(serverKexInit.length);
		hash.putBytes(serverKexInit);

		// The host key
		if(hostKey==null) {
			hash.putInt(0);
		} else {
			hash.putInt(hostKey.length);
			hash.putBytes(hostKey);
		}

		// The diffie hellman e value
		hash.putBigInteger(e);

		// The diffie hellman f value
		hash.putBigInteger(f);

		// The diffie hellman k value
		hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();

	}

}

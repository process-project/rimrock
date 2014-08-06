//Changes (c) STFC/CCLRC 2006-2007
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;

import org.bouncycastle.util.encoders.Base64;
import org.globus.gsi.gssapi.GSSConstants;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.6 $
 */
public class SshKeyExchangeFactory {
	private static Map kexs;
	private static Map kexsNF;
	private static String defaultAlgorithm;
	private static String defaultAlgorithmNotFirst;
	private static Log log = LogFactory.getLog(SshKeyExchangeFactory.class);

	static {
		kexs = new HashMap();
		kexsNF = new HashMap();

		log.info("Loading key exchange methods");

		defaultAlgorithm = "diffie-hellman-group1-sha1";
		defaultAlgorithmNotFirst = "diffie-hellman-group1-sha1";

		try {
			String hash = new String(Base64.encode(MessageDigest.getInstance(
					"MD5").digest(GSSConstants.MECH_OID.getDER())), "US-ASCII");
			;
			kexs.put("gss-group1-sha1-" + hash, GssGroup1Sha1.class);
			kexsNF.put("gss-group1-sha1-" + hash, GssGroup1Sha1.class);
			defaultAlgorithm = "gss-group1-sha1-" + hash;
			defaultAlgorithmNotFirst = "gss-group1-sha1-" + hash;
		} catch (Exception e) {
			log.warn("Could not load gss-group1-sha1-* encoding:\n" + e);
		}

		kexs.put("diffie-hellman-group1-sha1", DhGroup1Sha1.class);
		kexsNF.put("diffie-hellman-group1-sha1", DhGroup1Sha1.class);

	}

	/**
	 * Creates a new SshKeyExchangeFactory object.
	 */
	protected SshKeyExchangeFactory() {
	}

	/**
   *
   */
	public static void initialize() {
	}

	/**
	 *
	 *
	 * @return
	 */
	public static String getDefaultKeyExchange() {
		return defaultAlgorithm;
	}

	public static String getDefaultKeyExchange(boolean firstExch) {
		if (firstExch)
			return defaultAlgorithm;
		else
			return defaultAlgorithmNotFirst;
	}

	/**
	 *
	 *
	 * @return
	 */
	public static List getSupportedKeyExchanges() {
		return new ArrayList(kexs.keySet());
	}

	/**
	 *
	 *
	 * @return
	 */
	public static List getSupportedKeyExchanges(boolean firstExch) {
		if (firstExch)
			return new ArrayList(kexs.keySet());
		else
			return new ArrayList(kexsNF.keySet());
	}

	/**
	 *
	 *
	 * @param methodName
	 *
	 * @return
	 *
	 * @throws AlgorithmNotSupportedException
	 */
	public static SshKeyExchange newInstance(String methodName, SshConnectionProperties properties)
			throws AlgorithmNotSupportedException {
		try {
			SshKeyExchange n = (SshKeyExchange) ((Class) kexs.get(methodName))
					.newInstance();
			n.setProperties(properties);
			return n;
		} catch (Exception e) {
			throw new AlgorithmNotSupportedException(methodName
					+ " is not supported!");
		}
	}
}

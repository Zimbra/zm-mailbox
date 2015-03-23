/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap.unboundid;

import javax.net.SocketFactory;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;

/**
 * @author sankumar
 *
 */
public class ServerSetConfig {
	private final String[] hosts;
	private final int[] ports;
	private final SocketFactory socketFactory;
	private final LDAPConnectionOptions ldapConnectionOptions;

	/**
	 * @return the hosts
	 */
	public String[] getHosts() {
		return hosts;
	}

	/**
	 * @return the ports
	 */
	public int[] getPorts() {
		return ports;
	}

	/**
	 * @return the socketFactory
	 */
	public SocketFactory getSocketFactory() {
		return socketFactory;
	}

	/**
	 * @return the ldapConnectionOptions
	 */
	public LDAPConnectionOptions getLdapConnectionOptions() {
		return ldapConnectionOptions;
	}

	private ServerSetConfig(ServerSetConfigBuider builder) {
		this.hosts = builder.hosts;
		this.ports = builder.ports;
		this.socketFactory = builder.socketFactory;
		this.ldapConnectionOptions = builder.ldapConnectionOptions;
	}

	public static class ServerSetConfigBuider {
		private String[] hosts;
		private int[] ports;
		private SocketFactory socketFactory;
		private LDAPConnectionOptions ldapConnectionOptions;

		public ServerSetConfigBuider setHosts(String[] hosts) {
			this.hosts = hosts;
			return this;

		}

		public ServerSetConfigBuider setPorts(int[] ports) {
			this.ports = ports;
			return this;

		}

		public ServerSetConfigBuider setSocketFactory(
				SocketFactory socketFactory) {
			this.socketFactory = socketFactory;
			return this;
		}

		public ServerSetConfigBuider setLDAPConnectionOptions(
				LDAPConnectionOptions ldapConnectionOptions) {
			this.ldapConnectionOptions = ldapConnectionOptions;
			return this;
		}

		public ServerSetConfig build() {
			return new ServerSetConfig(this);
		}

	}
}

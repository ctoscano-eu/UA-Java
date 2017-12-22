/*
 * ======================================================================== Copyright (c) 2005-2015
 * The OPC Foundation, Inc. All rights reserved.
 *
 * OPC Foundation MIT License 1.00
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * The complete license agreement can be found here: http://opcfoundation.org/License/MIT/1.00/
 * ======================================================================
 */

package org.opcfoundation.ua.examples;

import java.util.Arrays;

import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.TimestampsToReturn;

import pt.inesctec.opcua.MyCLient;

/**
 * Sample client creates a connection to OPC UA Server (1st arg), browses and reads a boolean value. It is configured to work against NanoServer example, using the address opc.tcp://localhost:8666/
 * 
 * NOTE: Does not work against SeverExample1, since it does not support Browse
 */
public class SampleClient {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: SampleClient [server uri]");
			return;
		}
		String url = args[0];
		System.out.print("SampleClient: Connecting to " + url + " .. ");

		MyCLient myClient = new MyCLient();
		myClient.create("SampleClient");

		SessionChannel mySession = myClient.createSession(url);

		///////////// EXECUTE //////////////
		// Browse Root
		BrowseDescription browse = new BrowseDescription();
		browse.setNodeId(Identifiers.RootFolder);
		browse.setBrowseDirection(BrowseDirection.Forward);
		browse.setIncludeSubtypes(true);
		browse.setNodeClassMask(NodeClass.Object, NodeClass.Variable);
		browse.setResultMask(BrowseResultMask.All);
		BrowseResponse res3 = mySession.Browse(null, null, null, browse);
		System.out.println(res3);

		// Read Namespace Array
		ReadResponse res5 = mySession.Read(null, null, TimestampsToReturn.Neither, new ReadValueId(Identifiers.Server_NamespaceArray, Attributes.Value, null, null));
		String[] namespaceArray = (String[]) res5.getResults()[0].getValue().getValue();
		System.out.println(Arrays.toString(namespaceArray));

		// Read a variable (Works with NanoServer example!)
		ReadResponse res4 = mySession.Read(null, 500.0, TimestampsToReturn.Source, 
				new ReadValueId(new NodeId(1, 1007), Attributes.Value, null, null),
		    new ReadValueId(new NodeId(1, 1006), Attributes.Value, null, null), 
		    new ReadValueId(new NodeId(1, "Boolean"), Attributes.Value, null, null));
		System.out.println(res4);

		myClient.shutdownSession();
	}

}

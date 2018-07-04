package pt.inesctec.opcua;

import org.opcfoundation.ua.builtintypes.NodeId;

public class MyBrowsePath {

	public NodeId startingNode;
	public StringBuffer[] nameOfEachNode; // examples: "Objects", "Objects/Server", "Objects/Server/ServerStatus"
	public StringBuffer[] nsOfEachNode; // "0", "0/0", "0/0/0"

	public MyBrowsePath(NodeId startingNode) {
		this.startingNode = startingNode;
	}
	
	public int nrElements() {
		return nameOfEachNode.length;
	}

	//examples: "0/Objects", "0/Objects/0/Server", "0/Objects/0/Server/0/ServerStatus"
	public void setElements(String... elements) {
		nameOfEachNode = new StringBuffer[elements.length];
		nsOfEachNode = new StringBuffer[elements.length];

		for (int i = 0; i < elements.length; ++i) {
			String[] terms = elements[i].split("/");
			nameOfEachNode[i] = new StringBuffer();
			nsOfEachNode[i] = new StringBuffer();
			for (int j = 0; j < terms.length; ++j) {
				if ((j % 2) == 0) {
					if (nsOfEachNode[i].length() > 0)
						nsOfEachNode[i].append("/");
					nsOfEachNode[i].append(terms[j]);
				}
				else {
					if (nameOfEachNode[i].length() > 0)
						nameOfEachNode[i].append("/");
					nameOfEachNode[i].append(terms[j]);
				}
			}
		}
	}

	public String[][] getTermsArray() {
		String[][] termsArray = new String[nameOfEachNode.length][];
		for (int i = 0; i < nameOfEachNode.length; ++i) {
			String[] terms = nameOfEachNode[i].toString().split("/");
			if (terms.length == 0)
				return null;
			termsArray[i] = terms;
		}
		return termsArray;
	}

	public Integer[][] getNameSpacesArray() {
		Integer[][] nsArray = new Integer[nsOfEachNode.length][];
		for (int i = 0; i < nsOfEachNode.length; ++i) {
			String[] terms = nsOfEachNode[i].toString().split("/");
			if (terms.length == 0)
				return null;
			Integer[] numbers = new Integer[terms.length];
			for (int j = 0; j < terms.length; ++j)
				numbers[j] = Integer.valueOf(terms[j]);

			nsArray[i] = numbers;
		}
		return nsArray;
	}
}

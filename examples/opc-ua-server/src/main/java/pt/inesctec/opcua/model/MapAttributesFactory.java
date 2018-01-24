package pt.inesctec.opcua.model;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;

public class MapAttributesFactory {

	static public Map<UnsignedInteger, DataValue> buildMapAttributesForObject(NodeId nodeId, String browseName, DateTime serverTimeStamp) {
		HashMap<UnsignedInteger, DataValue> map = new HashMap<UnsignedInteger, DataValue>();

		map.put(Attributes.NodeId, new DataValue(new Variant(nodeId), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.BrowseName, new DataValue(new Variant(qualifiedName(browseName)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText(browseName, LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.Description, new DataValue(new Variant(new LocalizedText(browseName, LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.EventNotifier, new DataValue(new Variant(new UnsignedByte(0)), StatusCode.GOOD, null, serverTimeStamp));

		return map;
	}

	static public Map<UnsignedInteger, DataValue> buildMapAttributesForObjectType(NodeId nodeId, String browseName, DateTime serverTimeStamp) {
		HashMap<UnsignedInteger, DataValue> map = new HashMap<UnsignedInteger, DataValue>();

		map.put(Attributes.NodeId, new DataValue(new Variant(nodeId), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ObjectType), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.BrowseName, new DataValue(new Variant(qualifiedName(browseName)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText(browseName, LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.Description, new DataValue(new Variant(new LocalizedText(browseName, LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));

		return map;
	}

	static public Map<UnsignedInteger, DataValue> buildMapAttributesForVariable(NodeId nodeId, String browseName, Object value, NodeId nodeIdForvariableType, DateTime serverTimeStamp) {
		HashMap<UnsignedInteger, DataValue> map = new HashMap<UnsignedInteger, DataValue>();

		map.put(Attributes.NodeId, new DataValue(new Variant(nodeId), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.BrowseName, new DataValue(new Variant(qualifiedName(browseName)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText(browseName, LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.Description, new DataValue(new Variant(new LocalizedText(browseName, LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.Value, new DataValue(new Variant(value), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.DataType, new DataValue(new Variant(nodeIdForvariableType), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.ValueRank, new DataValue(new Variant(-1), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.ArrayDimensions, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.AccessLevel, new DataValue(new Variant(new UnsignedByte(AccessLevel.CurrentRead.getValue())), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.UserAccessLevel, new DataValue(new Variant(new UnsignedByte(AccessLevel.CurrentRead.getValue())), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.MinimumSamplingInterval, new DataValue(new Variant(1000.0), StatusCode.GOOD, null, serverTimeStamp));
		map.put(Attributes.Historizing, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));

		return map;
	}

	static private QualifiedName qualifiedName(String name) {
		return new QualifiedName(1, name);
	}

}
